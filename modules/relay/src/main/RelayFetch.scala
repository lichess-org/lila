package lila.relay

import akka.actor._
import org.joda.time.DateTime
import play.api.libs.ws.WS
import play.api.Play.current
import scala.concurrent.duration._

import lila.common.LilaException

private final class RelayFetch(
    sync: RelaySync,
    getSyncable: () => Fu[List[Relay]],
    addLog: (Relay.Id, SyncLog.Event) => Funit
) extends Actor {

  override def preStart {
    logger.info("Start RelaySync")
    context setReceiveTimeout 15.seconds
    scheduleNext
  }

  case object Tick

  def scheduleNext =
    context.system.scheduler.scheduleOnce(3 seconds, self, Tick)

  def receive = {

    case ReceiveTimeout =>
      val msg = "RelaySync timed out!"
      logger.error(msg)
      throw new RuntimeException(msg)

    case Tick =>
      val startAt = nowMillis
      getSyncable().flatMap {
        _.map { relay =>
          RelayFetch(relay.sync.upstream)
            .withTimeout(3 seconds, LilaException(s"Request timeout"))(context.system) flatMap {
              sync(relay, _)
            } flatMap { res =>
              addLog(relay.id, SyncLog.Event(none, DateTime.now)) inject res
            } recover {
              case e: Exception => addLog(relay.id, SyncLog.Event(e.getMessage.some, DateTime.now))
            }
        }.sequenceFu.chronometer
          .logIfSlow(3000, logger)(_ => "RelaySync.tick")
          .result addEffectAnyway scheduleNext
      }
  }
}

private object RelayFetch {

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

  private def dgtOneFile(file: String): Fu[MultiPgn] =
    WS.url(file).get().flatMap {
      case res if res.status == 200 => fuccess(MultiPgn(res.body))
      case res => fufail(s"Cannot fetch $file (error ${res.status})")
    }

  private def dgtManyFiles(dir: String): Fu[MultiPgn] = {
    val roundUrl = s"$dir/round.json"
    WS.url(roundUrl).get() flatMap {
      case res if res.status == 200 => roundReads reads res.json match {
        case JsError(err) => fufail(err.toString)
        case JsSuccess(round, _) => (1 to round.pairings.size).map { number =>
          val gameUrl = s"$dir/game-$number.pgn"
          WS.url(gameUrl).get().flatMap {
            case res if res.status == 200 => fuccess(res.body)
            case res => fufail(s"Cannot fetch $gameUrl (error ${res.status})")
          }
        }.sequenceFu map { games =>
          MultiPgn(games.mkString("\n\n"))
        }
      }
      case res => fufail(s"Cannot fetch $roundUrl (error ${res.status})")
    }
  }
}
