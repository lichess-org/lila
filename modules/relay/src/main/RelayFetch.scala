package lila.relay

import akka.actor._
import com.github.blemale.scaffeine.{ Cache, Scaffeine }
import io.lemonlabs.uri.Url
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.ws.{ WS, WSResponse }
import play.api.Play.current
import scala.concurrent.duration._

import chess.format.pgn.Tags
import lila.base.LilaException
import lila.study.MultiPgn
import lila.tree.Node.Comments
import Relay.Sync.Upstream

private final class RelayFetch(
    sync: RelaySync,
    api: RelayApi,
    slackApi: lila.slack.SlackApi,
    formatApi: RelayFormatApi,
    chapterRepo: lila.study.ChapterRepo
) extends Actor {

  override def preStart: Unit = {
    logger.info("Start RelaySync")
    context setReceiveTimeout 20.seconds
    context.system.scheduler.scheduleOnce(10.seconds)(scheduleNext)
  }

  case object Tick

  def scheduleNext =
    context.system.scheduler.scheduleOnce(600 millis, self, Tick)

  def receive = {

    case ReceiveTimeout =>
      val msg = "RelaySync timed out!"
      logger.error(msg)
      throw new RuntimeException(msg)

    case Tick => api.toSync.flatMap { relays =>
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
    else doProcess(relay) flatMap { games =>
      sync(relay, games)
        .chronometer.mon(_.relay.sync.duration.each).result
        .withTimeout(1500 millis, SyncResult.Timeout)(context.system) map { res =>
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

  def continueRelay(r: Relay): Fu[Relay] = {
    if (r.sync.log.alwaysFails && !r.sync.upstream.isLocal) {
      r.sync.log.events.lastOption.flatMap(_.error).ifTrue(r.official && r.hasStarted) foreach { error =>
        slackApi.broadcastError(r.id.value, r.name, error)
      }
      fuccess(60)
    } else r.sync.delay match {
      case Some(delay) => fuccess(delay)
      case None => api getNbViewers r map { nb =>
        (18 - nb) atLeast 7
      }
    }
  } map { seconds =>
    r.withSync {
      _.copy(
        nextAt = DateTime.now plusSeconds {
          seconds atLeast { if (r.sync.log.justTimedOut) 10 else 2 }
        } some
      )
    }
  }

  import RelayFetch.GamesSeenBy

  private def doProcess(relay: Relay): Fu[RelayGames] =
    cache getIfPresent relay.sync.upstream match {
      case Some(GamesSeenBy(games, seenBy)) if !seenBy(relay.id) =>
        cache.put(relay.sync.upstream, GamesSeenBy(games, seenBy + relay.id))
        games
      case x =>
        val games = doFetch(relay.sync.upstream, RelayFetch.maxChapters(relay))
        cache.put(relay.sync.upstream, GamesSeenBy(games, Set(relay.id)))
        games
    }

  private val cache: Cache[Upstream, GamesSeenBy] = Scaffeine()
    .expireAfterWrite(30.seconds)
    .build[Upstream, GamesSeenBy]

  private def doFetch(upstream: Upstream, max: Int): Fu[RelayGames] = {
    import RelayFetch.DgtJson._
    formatApi.get(upstream.url) flatMap {
      case RelayFormat.SingleFile(doc) => doc.format match {
        // all games in a single PGN file
        case RelayFormat.DocFormat.Pgn => httpGet(doc.url) map { MultiPgn.split(_, max) }
        // maybe a single JSON game? Why not
        case RelayFormat.DocFormat.Json => httpGetJson[GameJson](doc.url)(gameReads) map { game =>
          MultiPgn(List(game.toPgn()))
        }
      }
      case RelayFormat.ManyFiles(indexUrl, makeGameDoc) => httpGetJson[RoundJson](indexUrl) flatMap { round =>
        round.pairings.zipWithIndex.map {
          case (pairing, i) =>
            val number = i + 1
            val gameDoc = makeGameDoc(number)
            (gameDoc.format match {
              case RelayFormat.DocFormat.Pgn => httpGet(gameDoc.url)
              case RelayFormat.DocFormat.Json => httpGetJson[GameJson](gameDoc.url) map { _.toPgn(pairing.tags) }
            }) map (number -> _)
        }.sequenceFu.map { results =>
          MultiPgn(results.sortBy(_._1).map(_._2).toList)
        }
      }
    } flatMap RelayFetch.multiPgnToGames.apply
  }

  private def httpGet(url: Url): Fu[String] =
    WS.url(url.toString).withRequestTimeout(4.seconds.toMillis).get().flatMap {
      case res if res.status == 200 => fuccess(res.body)
      case res => fufail(s"[${res.status}] $url")
    }

  private def httpGetJson[A: Reads](url: Url): Fu[A] = for {
    str <- httpGet(url)
    json <- scala.concurrent.Future(Json parse str) // Json.parse throws exceptions (!)
    data <- implicitly[Reads[A]].reads(json).fold(
      err => fufail(s"Invalid JSON from $url: $err"),
      fuccess
    )
  } yield data
}

private object RelayFetch {

  case class GamesSeenBy(games: Fu[RelayGames], seenBy: Set[Relay.Id])

  def maxChapters(relay: Relay) =
    lila.study.Study.maxChapters * (if (relay.official) 2 else 1)

  private object DgtJson {
    case class PairingPlayer(fname: Option[String], mname: Option[String], lname: Option[String], title: Option[String]) {
      def fullName = some {
        List(fname, mname, lname).flatten mkString " "
      }.filter(_.nonEmpty)
    }
    case class RoundJsonPairing(white: PairingPlayer, black: PairingPlayer, result: String) {
      import chess.format.pgn._
      def tags = Tags(List(
        white.fullName map { v => Tag(_.White, v) },
        white.title map { v => Tag(_.WhiteTitle, v) },
        black.fullName map { v => Tag(_.Black, v) },
        black.title map { v => Tag(_.BlackTitle, v) },
        Tag(_.Result, result).some
      ).flatten)
    }
    case class RoundJson(pairings: List[RoundJsonPairing])
    implicit val pairingPlayerReads = Json.reads[PairingPlayer]
    implicit val roundPairingReads = Json.reads[RoundJsonPairing]
    implicit val roundReads = Json.reads[RoundJson]

    case class GameJson(moves: List[String], result: Option[String]) {
      def toPgn(extraTags: Tags = Tags.empty) = {
        val strMoves = moves.map(_ split ' ') map { move =>
          chess.format.pgn.Move(
            san = ~move.headOption,
            secondsLeft = move.lift(1).map(_.takeWhile(_.isDigit)) flatMap parseIntOption
          )
        } mkString " "
        s"${extraTags}\n\n$strMoves"
      }
    }
    implicit val gameReads = Json.reads[GameJson]
  }
  import DgtJson._

  private object multiPgnToGames {

    import scala.util.{ Try, Success, Failure }
    import com.github.blemale.scaffeine.{ LoadingCache, Scaffeine }

    def apply(multiPgn: MultiPgn): Fu[List[RelayGame]] =
      multiPgn.value.foldLeft[Try[(List[RelayGame], Int)]](Success(List.empty -> 0)) {
        case (Success((acc, index)), pgn) => pgnCache.get(pgn) flatMap { f =>
          val game = f(index)
          if (game.isEmpty) Failure(LilaException(s"Found an empty PGN at index $index"))
          else Success(game :: acc, index + 1)
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
