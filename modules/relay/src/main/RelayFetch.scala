package lila.relay

import akka.actor._
import org.joda.time.DateTime
import play.api.libs.ws.{ WS, WSResponse }
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

  override def preStart {
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

    case Tick =>
      api.unfinished.map(_ filter RelayFetch.shouldFetchNow).flatMap { relays =>
        lila.mon.relay.unfinished(relays.size)
        relays.map { relay =>
          if (relay.sync.playing) RelayFetch(relay.sync.upstream, relay.id)
            .chronometer.mon(_.relay.fetch.duration.each).result flatMap { games =>
              sync(relay, games)
                .chronometer.mon(_.relay.sync.duration.each).result
                .withTimeout(500 millis, SyncResult.Timeout)(context.system) flatMap { res =>
                  api.addLog(relay.id, SyncLog.event(res.moves, none)) inject res
                }
            } recoverWith {
              case e: Exception =>
                api.addLog(relay.id, SyncLog.event(0, e.some)) inject (e match {
                  case res @ SyncResult.Timeout => res
                  case _ => SyncResult.Error(e.getMessage)
                })
            } flatMap updateRelay(relay)
          else finishNotSyncing(relay)
        }.sequenceFu.chronometer
          .mon(_.relay.sync.duration.total)
          .result addEffectAnyway scheduleNext
      }
  }

  def updateRelay(r: Relay)(result: SyncResult): Funit =  api byId id flatMap {
    _ ?? { r =>
    lila.mon.relay.sync.result(result.toString.toLowerCase)()
    result match {
      case SyncResult.Ok(0, games) =>
        chapterRepo.relaysAndTagsByStudyId(r.studyId) map { chapters =>
          relay.startedAt ?? { _ isBefore DateTime.now.minusMinutes(
          if (chapters.isEmpty) relay.DateTime.now
          chapters forall (_.looksOver)
        } flatMap {
          _ ?? api.updateIfChanged(r)(_.finish)
        }
      case SyncResult.Ok(nbMoves, games) =>
        lila.mon.relay.moves(nbMoves)
        api.update(r)(_.ensureStarted)
      case SyncResult.Timeout => continueRelay(r)
      case SyncResult.Error(_) => continueRelay(r)
    }
  } flatMap { newRelay =>
    (newRelay != r) ?? api.update(newRelay, from = r.some)
  }

  def continueRelay(r: Relay): Fu[Relay] =
    (if (r.sync.log.alwaysFails) fuccess(30) else (r.sync.delay match {
      case Some(delay) => fuccess(delay)
      case None => api.getNbViewers(r) map {
        case 0 => 30
        case nb => (16 - nb) atLeast 5
      }
    })) map { seconds =>
      r.setUnFinished.withSync(_.copy(nextAt = DateTime.now plusSeconds {
        seconds atLeast { if (r.sync.log.isOk) 5 else 10 }
      } some))
    }

  def finishRelay(r: Relay, nbMoves: Int, games: RelayGames): Fu[Option[Relay]] =
    if (nbMoves > 0) fuccess(none)
    else if (games.forall(!_.started)) fuccess(none)
    else chapterRepo.relaysAndTagsByStudyId(r.studyId) map { chapters =>
      // probably TCEC style where single file/URL is used for many games in a row
      if (games.size == 1) chapters forall (_.looksOver)
      else games.forall { game =>
        chapters.find(_.relay.index == game.index) ?? (_.looksOver)
      }
    } map (_ option r.setFinished)

  def finishNotSyncing(r: Relay): Funit =
    chapterRepo.relaysAndTagsByStudyId(r.studyId) map {
      _ forall (_.looksOver)
    } flatMap {
      _ ?? api.update(r.setFinished, from = r.some)
    }
}

private object RelayFetch {

  case class MultiPgn(value: List[String]) extends AnyVal

  def shouldFetchNow(r: Relay) = !r.sync.log.alwaysFails || {
    r.sync.log.updatedAt ?? { DateTime.now.minusSeconds(30).isAfter }
  }

  import Relay.Sync.Upstream
  case class GamesSeenBy(games: Fu[RelayGames], seenBy: Set[Relay.Id])

  def apply(upstream: Upstream, relayId: Relay.Id): Fu[RelayGames] =
    cache getIfPresent upstream match {
      case Some(GamesSeenBy(games, seenBy)) if !seenBy(relayId) =>
        cache.put(upstream, GamesSeenBy(games, seenBy + relayId))
        games
      case x =>
        val games = doFetch(upstream)
        cache.put(upstream, GamesSeenBy(games, Set(relayId)))
        games
    }

  import com.github.blemale.scaffeine.{ Cache, Scaffeine }
  private val cache: Cache[Upstream, GamesSeenBy] = Scaffeine()
    .expireAfterWrite(30.seconds)
    .build[Upstream, GamesSeenBy]

  private def doFetch(upstream: Upstream): Fu[RelayGames] = (upstream match {
    case Upstream.DgtOneFile(file) => dgtOneFile(file)
    case Upstream.DgtManyFiles(dir) => dgtManyFiles(dir)
  }) flatMap multiPgnToGames.apply

  private def dgtOneFile(file: String): Fu[MultiPgn] =
    httpGet(file).flatMap {
      case res if res.status == 200 => fuccess(splitPgn(res.body))
      case res => fufail(s"Cannot fetch $file (error ${res.status})")
    }

  import play.api.libs.json._

  private case class RoundJsonPairing(live: Boolean)
  private case class RoundJson(pairings: List[RoundJsonPairing])
  private implicit val roundPairingReads = Json.reads[RoundJsonPairing]
  private implicit val roundReads = Json.reads[RoundJson]

  private def dgtManyFiles(dir: String): Fu[MultiPgn] = {
    val roundUrl = s"$dir/round.json"
    httpGet(roundUrl) flatMap {
      case res if res.status == 200 => roundReads reads res.json match {
        case JsError(err) => fufail(err.toString)
        case JsSuccess(round, _) => (1 to round.pairings.size).map { number =>
          val gameUrl = s"$dir/game-$number.pgn"
          httpGet(gameUrl).flatMap {
            case res if res.status == 200 => fuccess(number -> res.body)
            case res => fufail(s"Cannot fetch $gameUrl (error ${res.status})")
          }
        }.sequenceFu map { results =>
          MultiPgn(results.sortBy(_._1).map(_._2).toList)
        }
      }
      case res => fufail(s"Cannot fetch $roundUrl (error ${res.status})")
    }
  }

  private def httpGet(url: String) = WS.url(url).withRequestTimeout(4.seconds.toMillis).get()

  private def splitPgn(str: String) = MultiPgn {
    """\n\n\[""".r.split(str.replace("\r\n", "\n")).toList match {
      case first :: rest => first :: rest.map(t => s"[$t")
      case Nil => Nil
    }
  }

  private object multiPgnToGames {

    import scala.util.{ Try, Success, Failure }
    import com.github.blemale.scaffeine.{ LoadingCache, Scaffeine }

    def apply(multiPgn: MultiPgn): Fu[List[RelayGame]] =
      multiPgn.value.zipWithIndex.foldLeft[Try[List[RelayGame]]](Success(List.empty)) {
        case (Success(acc), (pgn, index)) => pgnCache.get(pgn) map (f => f(index) :: acc)
        case (acc, _) => acc
      }.map(_.reverse).future

    private val pgnCache: LoadingCache[String, Try[Int => RelayGame]] = Scaffeine()
      .expireAfterAccess(2 minutes)
      .build(compute)

    private def compute(pgn: String): Try[Int => RelayGame] =
      lila.study.PgnImport(pgn, Nil).fold(
        err => Failure(LilaException(err)),
        res => Success(index => RelayGame(
          index = index,
          tags = res.tags,
          root = res.root.copy(
            comments = Comments.empty,
            children = res.root.children.updateMainline(_.copy(comments = Comments.empty))
          ),
          end = res.end
        ))
      )
  }
}
