package lila.relay

import akka.actor._
import org.joda.time.DateTime
import play.api.libs.ws.{ WS, WSResponse }
import play.api.Play.current
import scala.concurrent.duration._

import lila.common.LilaException

private final class RelayFetch(
    sync: RelaySync,
    getSyncable: () => Fu[List[Relay]],
    addLog: (Relay.Id, SyncLog.Event) => Funit
) extends Actor {

  val frequency = 5.seconds

  override def preStart {
    logger.info("Start RelaySync")
    context setReceiveTimeout 15.seconds
    scheduleNext
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
      val fetcher = new RelayFetch.CachedFetcher
      getSyncable().map(_ filter fetcher.shouldFetchNow).flatMap {
        _.map { relay =>
          fetcher(relay.sync.upstream) flatMap {
            sync(relay, _)
          } flatMap { res =>
            addLog(relay.id, SyncLog.event(none)) inject res
          } recover {
            case e: Exception => addLog(relay.id, SyncLog.event(e.some))
          }
        }.sequenceFu.chronometer
          .logIfSlow(3000, logger)(_ => "RelaySync.tick")
          .result addEffectAnyway scheduleNext
      }
  }
}

private object RelayFetch {

  final class CachedFetcher {

    private type Url = String

    private val cache = scala.collection.mutable.Map[Url, Fu[WSResponse]]()

    def shouldFetchNow(r: Relay) = !r.sync.log.alwaysFails || {
      r.sync.log.updatedAt ?? { DateTime.now.minusSeconds(30).isAfter }
    }

    import Relay.Sync.Upstream
    import RelaySync.MultiPgn

    import play.api.libs.json._

    private case class RoundJsonPairing(live: Boolean)
    private case class RoundJson(pairings: List[RoundJsonPairing])
    private implicit val roundPairingReads = Json.reads[RoundJsonPairing]
    private implicit val roundReads = Json.reads[RoundJson]

    def apply(upstream: Upstream): Fu[MultiPgn] = upstream match {
      case Upstream.DgtOneFile(file) => dgtOneFile(file)
      case Upstream.DgtManyFiles(dir) => dgtManyFiles(dir)
    }

    private val httpTimeout = 4.seconds

    private def getCached(url: String): Fu[WSResponse] =
      cache.getOrElseUpdate(
        url,
        WS.url(url).withRequestTimeout(httpTimeout.toMillis).get()
      )

    private def dgtOneFile(file: String): Fu[MultiPgn] =
      getCached(file).flatMap {
        case res if res.status == 200 => fuccess(splitPgn(res.body))
        case res => fufail(s"Cannot fetch $file (error ${res.status})")
      }

    private def dgtManyFiles(dir: String): Fu[MultiPgn] = {
      val roundUrl = s"$dir/round.json"
      getCached(roundUrl) flatMap {
        case res if res.status == 200 => roundReads reads res.json match {
          case JsError(err) => fufail(err.toString)
          case JsSuccess(round, _) => (1 to round.pairings.size).map { number =>
            val gameUrl = s"$dir/game-$number.pgn"
            getCached(gameUrl).flatMap {
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

    private def splitPgn(str: String) = MultiPgn {
      """\n\n\[""".r.split(str.replace("\r\n", "\n")).toList match {
        case first :: rest => first :: rest.map(t => s"[$t")
        case Nil => Nil
      }
    }
  }
}
