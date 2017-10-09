package lila.relay

import akka.actor._
import org.joda.time.DateTime
import play.api.libs.ws.{ WS, WSResponse }
import play.api.Play.current
import scala.concurrent.duration._

import lila.common.LilaException

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
      api.toSync.map(_ filter RelayFetch.shouldFetchNow).flatMap { relays =>
        val fetcher = new RelayFetch.CachedFetcher(relays.size)
        relays.map { relay =>
          fetcher(relay.sync.upstream) flatMap { games =>
            sync(relay, games)
              .withTimeout(300 millis, SyncResult.Timeout)(context.system) flatMap { res =>
                api.addLog(relay.id, SyncLog.event(res.moves, none)) inject res
              }
          } recoverWith {
            case e: Exception =>
              api.addLog(relay.id, SyncLog.event(0, e.some)) inject (e match {
                case res @ SyncResult.Timeout => res
                case _ => SyncResult.Error(e.getMessage)
              })
          } flatMap updateRelay(relay.id)
        }.sequenceFu.chronometer
          .result addEffectAnyway scheduleNext
      }
  }

  def updateRelay(id: Relay.Id)(result: SyncResult): Funit = api byId id flatMap {
    _ ?? { r =>
      ((r.sync.until, r.sync.nextAt, result) match {
        case (Some(until), Some(nextAt), SyncResult.Ok(nbMoves, games)) =>
          if (until isBefore DateTime.now) fuccess(r.withSync(_ set false))
          else if (r.finished) fuccess(r.withSync(_ set false))
          else finishRelay(r, nbMoves, games) getOrElse {
            if (r.sync.log.alwaysFails) fuccess(r.withSync(_.copy(nextAt = DateTime.now plusSeconds 20 some)))
            else (r.sync.delay match {
              case Some(delay) => fuccess(delay)
              case None => api.getNbViewers(r) map {
                case 0 => 30
                case nb => (16 - nb) atLeast 5
              }
            }) map { seconds =>
              r.withSync(_.copy(nextAt = DateTime.now plusSeconds {
                seconds atLeast { if (r.sync.log.isOk) 5 else 10 }
              } some))
            }
          }
        case _ => fuccess(r)
      }) flatMap { newRelay =>
        (newRelay != r) ?? {
          if (newRelay.sync.until != r.sync.until) api.publishRelay(newRelay)
          api update newRelay
        }
      }
    }
  }

  private def finishRelay(r: Relay, nbMoves: Int, games: RelayGames): Fu[Option[Relay]] =
    if (r.finished) fuccess((nbMoves > 0) option r.setUnFinished)
    else if (nbMoves > 0) fuccess(none)
    else chapterRepo.relaysAndTagsByStudyId(r.studyId) map { chapters =>
      games.forall { game =>
        chapters.find(c => game is c._2) ?? {
          case (chapterRelay, tags) =>
            tags.resultColor.isDefined ||
              game.end.isDefined ||
              chapterRelay.lastMoveAt.isBefore {
                DateTime.now.minusMinutes {
                  tags.clockConfig.fold(60)(_.limitInMinutes.toInt atLeast 30 atMost 120)
                }
              }
        }
      } option r.setFinished
    }
}

private object RelayFetch {

  case class MultiPgn(value: List[String]) extends AnyVal

  def shouldFetchNow(r: Relay) = !r.sync.log.alwaysFails || {
    r.sync.log.updatedAt ?? { DateTime.now.minusSeconds(30).isAfter }
  }

  final class CachedFetcher(capacity: Int) {

    import Relay.Sync.Upstream

    def apply(upstream: Upstream): Fu[RelayGames] =
      cache.computeIfAbsent(upstream, fetchFunction)

    import java.util.concurrent.ConcurrentHashMap
    private val cache = new ConcurrentHashMap[Upstream, Fu[RelayGames]](capacity)

    private val fetchFunction = new java.util.function.Function[Upstream, Fu[RelayGames]] {
      def apply(u: Upstream) = doFetch(u)
    }

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
  }

  private object multiPgnToGames {

    import scala.util.{ Try, Success, Failure }
    import com.github.blemale.scaffeine.{ LoadingCache, Scaffeine }

    def apply(multiPgn: MultiPgn): Fu[List[RelayGame]] =
      multiPgn.value.foldLeft[Try[List[RelayGame]]](Success(List.empty)) {
        case (Success(acc), pgn) => pgnCache.get(pgn) map (_ :: acc)
        case (acc, _) => acc
      }.map(_.reverse).future

    private val pgnCache: LoadingCache[String, Try[RelayGame]] = Scaffeine()
      .expireAfterAccess(2 minutes)
      .build(compute)

    private def compute(pgn: String): Try[RelayGame] = for {
      res <- lila.study.PgnImport(pgn, Nil).fold(
        err => Failure(LilaException(err)),
        Success.apply
      )
      white <- res.tags(_.White) toTry LilaException("Missing PGN White tag")
      black <- res.tags(_.Black) toTry LilaException("Missing PGN Black tag")
    } yield RelayGame(
      tags = res.tags,
      root = res.root,
      end = res.end,
      whiteName = RelayGame.PlayerName(white),
      blackName = RelayGame.PlayerName(black)
    )
  }
}
