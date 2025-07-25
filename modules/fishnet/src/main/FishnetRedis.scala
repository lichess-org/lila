package lila.fishnet

import akka.actor.CoordinatedShutdown
import chess.format.Uci
import io.lettuce.core.*
import io.lettuce.core.pubsub.*

import lila.common.{ Bus, Lilakka }
import lila.core.round.{ Tell, RoundBus, FishnetStart }

final class FishnetRedis(
    client: RedisClient,
    chanIn: String,
    chanOut: String,
    shutdown: CoordinatedShutdown
)(using Executor):

  val connIn = client.connectPubSub()
  val connOut = client.connectPubSub()

  private var stopping = false

  def request(work: Work.Move): Unit =
    if !stopping then connOut.async.publish(chanOut, writeWork(work))

  connIn.async.subscribe(chanIn)

  connIn.addListener:
    new RedisPubSubAdapter[String, String]:
      override def message(chan: String, msg: String): Unit =
        msg.split(' ') match
          case Array("start") => Bus.pub(FishnetStart)
          case Array(gameId, sign, uci) =>
            Uci(uci).foreach { move =>
              Bus.pub(Tell(GameId(gameId), RoundBus.FishnetPlay(move, sign)))
            }
          case _ =>

  Lilakka.shutdown(shutdown, _.PhaseServiceUnbind, "Stopping the fishnet redis pool"): () =>
    Future:
      stopping = true
      client.shutdown()

  private def writeWork(work: Work.Move): String =
    List(
      work.game.id,
      work.level,
      work.clock.so(writeClock),
      work.game.variant.some.filter(_.exotic).so(_.key.value),
      work.game.initialFen.so(_.value),
      work.game.moves
    ).mkString(";")

  private def writeClock(clock: Work.Clock): String =
    List(
      clock.wtime,
      clock.btime,
      clock.inc
    ).mkString(" ")
