package lila.fishnet

import chess.format.Uci
import io.lettuce.core._
import io.lettuce.core.pubsub._

final class FishnetRedis(
    client: RedisClient,
    chanIn: String,
    chanOut: String,
    system: akka.actor.ActorSystem
) {

  val connIn = client.connectPubSub()
  val connOut = client.connectPubSub()

  def request(work: Work.Move): Unit = connOut.async.publish(chanOut, writeWork(work))

  connIn.async.subscribe(chanIn)

  connIn.addListener(new RedisPubSubAdapter[String, String] {
    override def message(chan: String, msg: String): Unit = msg split ' ' match {
      case Array(gameId, uci) => Uci(uci) foreach { move =>
        system.lilaBus.publish(
          lila.hub.actorApi.map.Tell(gameId, lila.hub.actorApi.round.FishnetPlay(move)),
          'roundMapTell
        )
      }
      case _ =>
    }
  })

  private def writeWork(work: Work.Move): String = List(
    work.game.id,
    work.level,
    work.clock ?? writeClock,
    work.game.variant.some.filter(_.exotic).??(_.key),
    work.game.initialFen.??(_.value),
    work.game.moves
  ) mkString ";"

  private def writeClock(clock: Work.Clock): String = List(
    clock.wtime,
    clock.btime,
    clock.inc
  ) mkString " "
}
