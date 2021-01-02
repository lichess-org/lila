package lila.fishnet

import chess.format.Uci
import io.lettuce.core._
import io.lettuce.core.pubsub._
import scala.concurrent.Future

import lila.hub.actorApi.map.{ Tell, TellAll }
import lila.hub.actorApi.round.{ FishnetPlay, FishnetStart }
import lila.common.{ Bus, Lilakka }
import akka.actor.CoordinatedShutdown

final class FishnetRedis(
    client: RedisClient,
    chanIn: String,
    chanOut: String,
    shutdown: CoordinatedShutdown
)(implicit ec: scala.concurrent.ExecutionContext) {

  val connIn  = client.connectPubSub()
  val connOut = client.connectPubSub()

  private var stopping = false

  def request(work: Work.Move): Unit =
    if (!stopping) connOut.async.publish(chanOut, writeWork(work))

  connIn.async.subscribe(chanIn)

  connIn.addListener(new RedisPubSubAdapter[String, String] {
    override def message(chan: String, msg: String): Unit =
      msg split ' ' match {

        case Array("start") => Bus.publish(TellAll(FishnetStart), "roundSocket")

        case Array(gameId, plyS, uci) =>
          for {
            move <- Uci(uci)
            ply  <- plyS.toIntOption
          } Bus.publish(Tell(gameId, FishnetPlay(move, ply)), "roundSocket")
        case _ =>
      }
  })

  Lilakka.shutdown(shutdown, _.PhaseServiceUnbind, "Stopping the fishnet redis pool") { () =>
    Future {
      stopping = true
      client.shutdown()
    }
  }

  private def writeWork(work: Work.Move): String =
    List(
      work.game.id,
      work.level,
      work.clock ?? writeClock,
      work.game.variant.some.filter(_.exotic).??(_.key),
      work.game.initialFen.??(_.value),
      work.game.moves
    ) mkString ";"

  private def writeClock(clock: Work.Clock): String =
    List(
      clock.wtime,
      clock.btime,
      clock.inc,
      clock.byo
    ) mkString " "
}
