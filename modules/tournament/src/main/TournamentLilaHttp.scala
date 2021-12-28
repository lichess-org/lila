package lila.tournament

import akka.actor._
import akka.stream.scaladsl._
import scala.concurrent.duration._
import lila.common.LilaStream
import io.lettuce.core.RedisClient
import play.api.libs.json.Json

final private class TournamentLilaHttp(
    api: TournamentApi,
    tournamentRepo: TournamentRepo,
    jsonView: JsonView,
    redisClient: RedisClient
)(implicit mat: akka.stream.Materializer, system: ActorSystem) {

  private val channel = "http-out"
  private val conn    = redisClient.connectPubSub()

  private val periodicSender = system.actorOf(Props(new Actor {

    implicit def ec = context.dispatcher

    override def preStart(): Unit = {
      context setReceiveTimeout 20.seconds
      context.system.scheduler.scheduleOnce(10 seconds, self, Tick).unit
    }

    case object Tick

    def scheduleNext(): Unit =
      context.system.scheduler.scheduleOnce(1 second, self, Tick).unit

    def receive = {

      case ReceiveTimeout =>
        val msg = "tournament.lilaHttp timed out!"
        logger.branch("lila-http").error(msg)
        throw new RuntimeException(msg)

      case Tick =>
        tournamentRepo
          .startedCursorWithNbPlayersGte(0)
          .documentSource()
          .mapAsyncUnordered(4)(jsonView.lilaHttp)
          .map { json =>
            conn.async.publish(channel, Json stringify json).unit
          }
          .toMat(LilaStream.sinkCount)(Keep.right)
          .run()
          .addEffect { nb =>
            lila.mon.tournament.started.update(nb).unit
          }
          .monSuccess(_.tournament.startedOrganizer.tick)
          .unit
    }
  }))
}
