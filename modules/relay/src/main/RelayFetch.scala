package lila.relay

import akka.actor._
import org.joda.time.DateTime
import play.api.libs.ws.WS
import play.api.Play.current
import scala.concurrent.duration._

import lila.base.LilaException
import lila.tree.Node.Comments

private final class RelayFetch(
    sync: RelaySync,
    api: RelayApi,
    chapterRepo: lila.study.ChapterRepo
) extends Actor {

  val frequency = 1.seconds

  override def preStart: Unit = {
    logger.info("Start RelaySync")
    context setReceiveTimeout 20.seconds
    context.system.scheduler.scheduleOnce(10.seconds)(scheduleNext)
  }

  case object Tick

  def scheduleNext =
    context.system.scheduler.scheduleOnce(frequency, self, Tick)

  def receive = {

    case ReceiveTimeout =>
      val msg = "RelaySync timed out!"
      logger.error(msg)
      throw new RuntimeException(msg)

    case Tick => api.toSync.map(_ take 1).flatMap { relays =>
      lila.mon.relay.ongoing(relays.size)
      relays.map { relay =>
        if (relay.sync.ongoing) processRelay(relay) flatMap { newRelay =>
          api.update(relay)(_ => newRelay)
        }
        else if (relay.hasStarted) {
          logger.info(s"Finish by lack of activity $relay")
          api.update(relay)(_.finish)
        } else if (relay.shouldGiveUp) {
          logger.info(s"Finish for lack of start $relay")
          api.update(relay)(_.finish)
        } else fuccess(relay)
      }.sequenceFu addEffectAnyway scheduleNext
    }
  }

  // no writing the relay; only reading!
  def processRelay(relay: Relay): Fu[Relay] =
    if (!relay.sync.playing) fuccess(relay.withSync(_.play))
    else RelayFetch(relay) flatMap { games =>
      sync(relay, games)
        .chronometer.mon(_.relay.sync.duration.each).result
        .withTimeout(1 second, SyncResult.Timeout)(context.system) map { res =>
          res -> relay.withSync(_ addLog SyncLog.event(res.moves, none))
        }
    } recover {
      case e: Exception => (e match {
        case res @ SyncResult.Timeout =>
          logger.info(s"Sync timeout $relay")
          res
        case _ =>
          logger.info(s"Sync error $relay ${e.getMessage take 80}")
          SyncResult.Error(e.getMessage)
      }) -> relay.withSync(_ addLog SyncLog.event(0, e.some))
    } flatMap {
      case (result, newRelay) => afterSync(result, newRelay)
    }

  def afterSync(result: SyncResult, relay: Relay): Fu[Relay] = {
    lila.mon.relay.sync.result(result.reportKey)()
    result match {
      case SyncResult.Ok(0, games) =>
        if (games.size > 1 && games.forall(_.finished)) {
          logger.info(s"Finish because all games are over $relay")
          fuccess(relay.finish)
        } else continueRelay(relay)
      case SyncResult.Ok(nbMoves, games) =>
        lila.mon.relay.moves(nbMoves)
        continueRelay(relay.ensureStarted.resume)
      case _ => continueRelay(relay)
    }
  }

  def continueRelay(r: Relay): Fu[Relay] =
    (if (r.sync.log.alwaysFails) fuccess(30) else (r.sync.delay match {
      case Some(delay) => fuccess(delay)
      case None => api.getNbViewers(r) map { nb =>
        if (r.sync.upstream.heavy) (18 - nb) atLeast 8
        else (13 - nb) atLeast 5
      }
    })) map { seconds =>
      r.withSync(_.copy(nextAt = DateTime.now plusSeconds {
        seconds atLeast { if (r.sync.log.isOk) 5 else 15 }
      } some))
    }
}

private object RelayFetch {

  case class MultiPgn(value: List[String]) extends AnyVal

  import Relay.Sync.Upstream
  case class GamesSeenBy(games: Fu[RelayGames], seenBy: Set[Relay.Id])

  def apply(relay: Relay): Fu[RelayGames] =
    cache getIfPresent relay.sync.upstream match {
      case Some(GamesSeenBy(games, seenBy)) if !seenBy(relay.id) =>
        cache.put(relay.sync.upstream, GamesSeenBy(games, seenBy + relay.id))
        games
      case x =>
        val games = doFetch(relay.sync.upstream, maxChapters(relay))
        cache.put(relay.sync.upstream, GamesSeenBy(games, Set(relay.id)))
        games
    }

  def maxChapters(relay: Relay) =
    lila.study.Study.maxChapters * relay.official.fold(2, 1)

  import com.github.blemale.scaffeine.{ Cache, Scaffeine }
  private val cache: Cache[Upstream, GamesSeenBy] = Scaffeine()
    .expireAfterWrite(30.seconds)
    .build[Upstream, GamesSeenBy]

  private def doFetch(upstream: Upstream, max: Int): Fu[RelayGames] = (upstream match {
    case Upstream.DgtOneFile(file) => dgtOneFile(file, max)
    case Upstream.DgtManyFiles(dir) => dgtManyFiles(dir, max)
  }) flatMap multiPgnToGames.apply

  private def dgtOneFile(file: String, max: Int): Fu[MultiPgn] =
    httpGet(file).flatMap {
      case res if res.status == 200 => fuccess(splitPgn(res.body, max))
      case res => fufail(s"[${res.status}]")
    }

  import play.api.libs.json._

  private case class RoundJsonPairing(live: Boolean)
  private case class RoundJson(pairings: List[RoundJsonPairing])
  private implicit val roundPairingReads = Json.reads[RoundJsonPairing]
  private implicit val roundReads = Json.reads[RoundJson]

  private def dgtManyFiles(dir: String, max: Int): Fu[MultiPgn] = {
    val roundUrl = s"$dir/round.json"
    httpGet(roundUrl) flatMap {
      case res if res.status == 200 => roundReads reads res.json match {
        case JsError(err) => fufail(err.toString)
        case JsSuccess(round, _) => (1 to round.pairings.size.atMost(max)).map { number =>
          val gameUrl = s"$dir/game-$number.pgn"
          httpGet(gameUrl).flatMap {
            case res if res.status == 200 => fuccess(number -> res.body)
            case res => fufail(s"[${res.status}] game-$number.pgn")
          }
        }.sequenceFu map { results =>
          MultiPgn(results.sortBy(_._1).map(_._2).toList)
        }
      }
      case res => fufail(s"[${res.status}] round.json")
    }
  }

  private def httpGet(url: String) = WS.url(url).withRequestTimeout(4.seconds.toMillis).get()

  private def splitPgn(str: String, max: Int) = MultiPgn {
    """\n\n\[""".r.split(str.replace("\r\n", "\n")).toList take max match {
      case first :: rest => first :: rest.map(t => s"[$t")
      case Nil => Nil
    }
  }

  private object multiPgnToGames {

    import scala.util.{ Try, Success, Failure }
    import com.github.blemale.scaffeine.{ LoadingCache, Scaffeine }

    def apply(multiPgn: MultiPgn): Fu[List[RelayGame]] =
      multiPgn.value.foldLeft[Try[(List[RelayGame], Int)]](Success(List.empty -> 0)) {
        case (Success((acc, index)), pgn) => pgnCache.get(pgn) map { f =>
          val game = f(index)
          if (game.isEmpty) acc -> index
          else (game :: acc, index + 1)
        }
        case (acc, _) => acc
      }.future.map(_._1.reverse)

    private val pgnCache: LoadingCache[String, Try[Int => RelayGame]] = Scaffeine()
      .expireAfterAccess(2 minutes)
      .build(compute)

    private def compute(pgn: String): Try[Int => RelayGame] =
      lila.study.PgnImport(pgn, Nil).fold(
        err => Failure(LilaException(err)),
        res => Success(index => RelayGame(
          index = index,
          tags = res.tags,
          variant = res.variant,
          root = res.root.copy(
            comments = Comments.empty,
            children = res.root.children.updateMainline(_.copy(comments = Comments.empty))
          ),
          end = res.end
        ))
      )
  }
}
