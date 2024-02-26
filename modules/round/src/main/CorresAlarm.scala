package lila.round

import akka.stream.scaladsl._
import org.joda.time.DateTime
import reactivemongo.akkastream.cursorProducer

import scala.concurrent.duration._

import lila.common.Bus
import lila.common.LilaStream
import lila.db.dsl._
import lila.game.{ Game, Pov }

final private class CorresAlarm(
    coll: Coll,
    hasUserId: (Game, lila.user.User.ID) => Fu[Boolean],
    proxyGame: Game.ID => Fu[Option[Game]]
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: akka.actor.ActorSystem
) {

  private case class Alarm(
      _id: String,       // game id
      ringsAt: DateTime, // when to notify the player
      expiresAt: DateTime
  )

  implicit private val AlarmHandler = reactivemongo.api.bson.Macros.handler[Alarm]

  private def scheduleNext(): Unit = system.scheduler.scheduleOnce(10 seconds) { run().unit }.unit

  system.scheduler.scheduleOnce(10 seconds) { scheduleNext() }

  Bus.subscribeFun("pauseGame") { case lila.game.actorApi.PauseGame(game) =>
    if (game.hasCorrespondenceClock && !game.hasAi) coll.delete.one($id(game.id)).unit
  }

  Bus.subscribeFun("finishGame") { case lila.game.actorApi.FinishGame(game, _, _) =>
    if (game.hasCorrespondenceClock && !game.hasAi) coll.delete.one($id(game.id)).unit
  }

  Bus.subscribeFun("moveEventCorres") {
    case lila.hub.actorApi.round.CorresMoveEvent(move, _, _, alarmable, _) if alarmable =>
      proxyGame(move.gameId) foreach {
        _ foreach { game =>
          game.bothPlayersHaveMoved ?? {
            game.playableCorrespondenceClock ?? { clock =>
              val remainingTime = clock remainingTime game.turnColor
              val ringsAt       = DateTime.now.plusSeconds(remainingTime.toInt * 8 / 10)
              coll.update
                .one(
                  $id(game.id),
                  Alarm(
                    _id = game.id,
                    ringsAt = ringsAt,
                    expiresAt = DateTime.now.plusSeconds(remainingTime.toInt * 2)
                  ),
                  upsert = true
                )
                .void
            }
          }
        }
      }
  }

  private def run(): Funit =
    coll.ext
      .find($doc("ringsAt" $lt DateTime.now))
      .cursor[Alarm]()
      .documentSource(200)
      .mapAsyncUnordered(4)(alarm => proxyGame(alarm._id))
      .via(LilaStream.collect)
      .mapAsyncUnordered(4) { game =>
        val pov = Pov(game, game.turnColor)
        pov.player.userId.fold(fuccess(true))(u => hasUserId(pov.game, u)).addEffect {
          case true  => // already looking at the game
          case false => Bus.publish(lila.game.actorApi.CorresAlarmEvent(pov), "corresAlarm")
        } >> coll.delete.one($id(game.id))
      }
      .toMat(LilaStream.sinkCount)(Keep.right)
      .run()
      .mon(_.round.alarm.time)
      .addEffectAnyway { scheduleNext() }
      .void
}
