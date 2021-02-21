package lila.relay

import akka.actor._
import chess.format.pgn.Tags
import com.github.blemale.scaffeine.LoadingCache
import io.lemonlabs.uri.Url
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.ws.StandaloneWSClient
import Relay.Sync.{ Upstream, UpstreamIds, UpstreamUrl }
import scala.concurrent.duration._

import lila.base.LilaException
import lila.memo.CacheApi
import lila.study.MultiPgn
import lila.tree.Node.Comments
import lila.game.{ Game, GameRepo, PgnDump }

final private class RelayFetch(
    sync: RelaySync,
    api: RelayApi,
    slackApi: lila.irc.SlackApi,
    formatApi: RelayFormatApi,
    gameRepo: GameRepo,
    pgnDump: PgnDump,
    ws: StandaloneWSClient
) extends Actor {

  implicit def system = context.system
  implicit def ec     = context.dispatcher

  override def preStart(): Unit = {
    context setReceiveTimeout 20.seconds
    context.system.scheduler.scheduleOnce(10.seconds)(scheduleNext())
    ()
  }

  case object Tick

  def scheduleNext(): Unit =
    context.system.scheduler.scheduleOnce(500 millis, self, Tick).unit

  def receive = {

    case ReceiveTimeout =>
      val msg = "RelaySync timed out!"
      logger.error(msg)
      throw new RuntimeException(msg)

    case Tick =>
      api.toSync.flatMap { relays =>
        List(true, false) foreach { official =>
          lila.mon.relay.ongoing(official).update(relays.count(_.official == official))
        }
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
        }.sequenceFu addEffectAnyway scheduleNext()
      }.unit
  }

  // no writing the relay; only reading!
  def processRelay(relay: Relay): Fu[Relay] =
    if (!relay.sync.playing) fuccess(relay.withSync(_.play))
    else
      fetchGames(relay)
        .mon(_.relay.fetchTime(relay.official, relay.slug))
        .addEffect(gs => lila.mon.relay.games(relay.official, relay.slug).update(gs.size).unit)
        .flatMap { games =>
          sync(relay, games)
            .withTimeout(7 seconds, SyncResult.Timeout)
            .mon(_.relay.syncTime(relay.official, relay.slug))
            .map { res =>
              res -> relay.withSync(_ addLog SyncLog.event(res.moves, none))
            }
        }
        .recover { case e: Exception =>
          (e match {
            case SyncResult.Timeout =>
              if (relay.official) logger.info(s"Sync timeout $relay")
              SyncResult.Timeout
            case _ =>
              if (relay.official) logger.info(s"Sync error $relay ${e.getMessage take 80}")
              SyncResult.Error(e.getMessage)
          }) -> relay.withSync(_ addLog SyncLog.event(0, e.some))
        }
        .map { case (result, newRelay) =>
          afterSync(result, newRelay)
        }

  def afterSync(result: SyncResult, relay: Relay): Relay =
    result match {
      case SyncResult.Ok(0, _) => continueRelay(relay)
      case SyncResult.Ok(nbMoves, _) =>
        lila.mon.relay.moves(relay.official, relay.slug).increment(nbMoves)
        continueRelay(relay.ensureStarted.resume)
      case _ => continueRelay(relay)
    }

  def continueRelay(r: Relay): Relay =
    r.sync.upstream.fold(r) { upstream =>
      val seconds =
        if (r.sync.log.alwaysFails && !upstream.local) {
          r.sync.log.events.lastOption
            .filterNot(_.isTimeout)
            .flatMap(_.error)
            .ifTrue(r.official && r.hasStarted) foreach { error =>
            slackApi.broadcastError(r.id.value, r.name, error)
          }
          60
        } else r.sync.delay getOrElse 7
      r.withSync {
        _.copy(
          nextAt = DateTime.now plusSeconds {
            seconds atLeast { if (r.sync.log.justTimedOut) 10 else 2 }
          } some
        )
      }
    }

  import com.github.benmanes.caffeine.cache.Cache
  import RelayFetch.GamesSeenBy

  private val gameIdsUpstreamPgnFlags = PgnDump.WithFlags(
    clocks = true,
    moves = true,
    tags = true,
    evals = false,
    opening = false,
    literate = false,
    pgnInJson = false,
    delayMoves = true
  )

  private def fetchGames(relay: Relay): Fu[RelayGames] =
    relay.sync.upstream ?? {
      case UpstreamIds(ids) =>
        gameRepo.gamesFromSecondary(ids) flatMap gameRepo.withInitialFens flatMap {
          _.map { case (game, fen) =>
            pgnDump(game, fen, gameIdsUpstreamPgnFlags).dmap(_.render)
          }.sequenceFu dmap MultiPgn.apply
        } flatMap RelayFetch.multiPgnToGames.apply
      case url: UpstreamUrl =>
        cache.asMap
          .compute(
            url,
            (_, v) =>
              Option(v) match {
                case Some(GamesSeenBy(games, seenBy)) if !seenBy(relay.id) =>
                  GamesSeenBy(games, seenBy + relay.id)
                case _ =>
                  GamesSeenBy(doFetchUrl(url, RelayFetch.maxChapters(relay)), Set(relay.id))
              }
          )
          .games
    }

  // The goal of this is to make sure that an upstream used by several broadcast
  // is only pulled from as many times as necessary, and not more.
  private val cache: Cache[UpstreamUrl, GamesSeenBy] = CacheApi.scaffeineNoScheduler
    .initialCapacity(4)
    .maximumSize(16)
    .build[UpstreamUrl, GamesSeenBy]()
    .underlying

  private def doFetchUrl(upstream: UpstreamUrl, max: Int): Fu[RelayGames] = {
    import RelayFetch.DgtJson._
    formatApi get upstream.withRound flatMap {
      case RelayFormat.SingleFile(doc) =>
        doc.format match {
          // all games in a single PGN file
          case RelayFormat.DocFormat.Pgn => httpGet(doc.url) map { MultiPgn.split(_, max) }
          // maybe a single JSON game? Why not
          case RelayFormat.DocFormat.Json =>
            httpGetJson[GameJson](doc.url)(gameReads) map { game =>
              MultiPgn(List(game.toPgn()))
            }
        }
      case RelayFormat.ManyFiles(indexUrl, makeGameDoc) =>
        httpGetJson[RoundJson](indexUrl) flatMap { round =>
          round.pairings.zipWithIndex
            .map { case (pairing, i) =>
              val number  = i + 1
              val gameDoc = makeGameDoc(number)
              (gameDoc.format match {
                case RelayFormat.DocFormat.Pgn => httpGet(gameDoc.url)
                case RelayFormat.DocFormat.Json =>
                  httpGetJson[GameJson](gameDoc.url) map { _.toPgn(pairing.tags) }
              }) map (number -> _)
            }
            .sequenceFu
            .map { results =>
              MultiPgn(results.sortBy(_._1).map(_._2))
            }
        }
    } flatMap RelayFetch.multiPgnToGames.apply
  }

  private def httpGet(url: Url): Fu[String] =
    ws.url(url.toString)
      .withRequestTimeout(4.seconds)
      .get()
      .flatMap {
        case res if res.status == 200 => fuccess(res.body)
        case res                      => fufail(s"[${res.status}] $url")
      }

  private def httpGetJson[A: Reads](url: Url): Fu[A] =
    for {
      str  <- httpGet(url)
      json <- scala.concurrent.Future(Json parse str) // Json.parse throws exceptions (!)
      data <-
        implicitly[Reads[A]]
          .reads(json)
          .fold(
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
    case class PairingPlayer(
        fname: Option[String],
        mname: Option[String],
        lname: Option[String],
        title: Option[String]
    ) {
      def fullName =
        some {
          List(fname, mname, lname).flatten mkString " "
        }.filter(_.nonEmpty)
    }
    case class RoundJsonPairing(white: PairingPlayer, black: PairingPlayer, result: String) {
      import chess.format.pgn._
      def tags =
        Tags(
          List(
            white.fullName map { v =>
              Tag(_.White, v)
            },
            white.title map { v =>
              Tag(_.WhiteTitle, v)
            },
            black.fullName map { v =>
              Tag(_.Black, v)
            },
            black.title map { v =>
              Tag(_.BlackTitle, v)
            },
            Tag(_.Result, result).some
          ).flatten
        )
    }
    case class RoundJson(pairings: List[RoundJsonPairing])
    implicit val pairingPlayerReads = Json.reads[PairingPlayer]
    implicit val roundPairingReads  = Json.reads[RoundJsonPairing]
    implicit val roundReads         = Json.reads[RoundJson]

    case class GameJson(moves: List[String], result: Option[String]) {
      def toPgn(extraTags: Tags = Tags.empty) = {
        val strMoves = moves.map(_ split ' ') map { move =>
          chess.format.pgn.Move(
            san = ~move.headOption,
            secondsLeft = move.lift(1).map(_.takeWhile(_.isDigit)) flatMap (_.toIntOption)
          )
        } mkString " "
        s"$extraTags\n\n$strMoves"
      }
    }
    implicit val gameReads = Json.reads[GameJson]
  }

  object multiPgnToGames {

    import scala.util.{ Failure, Success, Try }

    def apply(multiPgn: MultiPgn): Fu[Vector[RelayGame]] =
      multiPgn.value
        .foldLeft[Try[(Vector[RelayGame], Int)]](Success(Vector.empty -> 0)) {
          case (Success((acc, index)), pgn) =>
            pgnCache.get(pgn) flatMap { f =>
              val game = f(index)
              if (game.isEmpty) Failure(LilaException(s"Found an empty PGN at index $index"))
              else Success((acc :+ game, index + 1))
            }
          case (acc, _) => acc
        }
        .future
        .dmap(_._1)

    private val pgnCache: LoadingCache[String, Try[Int => RelayGame]] = CacheApi.scaffeineNoScheduler
      .expireAfterAccess(2 minutes)
      .maximumSize(512)
      .build(compute)

    private def compute(pgn: String): Try[Int => RelayGame] =
      lila.study
        .PgnImport(pgn, Nil)
        .fold(
          err => Failure(LilaException(err)),
          res =>
            Success(index =>
              RelayGame(
                index = index,
                tags = res.tags,
                variant = res.variant,
                root = res.root.copy(
                  comments = Comments.empty,
                  children = res.root.children.updateMainline(_.copy(comments = Comments.empty))
                ),
                end = res.end
              )
            )
        )
  }
}
