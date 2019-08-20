package lidraughts.round

import org.joda.time.DateTime
import play.api.libs.iteratee._
import reactivemongo.api._
import reactivemongo.play.iteratees.cursorProducer
import scala.concurrent.duration._

import lidraughts.common.Tellable
import lidraughts.db.dsl._
import lidraughts.game.{ Game, Pov }
import lidraughts.hub.actorApi.round.IsOnGame
import makeTimeout.short

private final class CorresAlarm(
    system: akka.actor.ActorSystem,
    coll: Coll,
    socketMap: SocketMap,
    proxyGame: Game.ID => Fu[Option[Game]]
) {

  private case class Alarm(
      _id: String, // game id
      ringsAt: DateTime, // when to notify the player
      expiresAt: DateTime
  )

  private implicit val AlarmHandler = reactivemongo.bson.Macros.handler[Alarm]

  private def scheduleNext: Unit = system.scheduler.scheduleOnce(10 seconds)(run)

  system.scheduler.scheduleOnce(10 seconds)(scheduleNext)

  system.lidraughtsBus.subscribeFun('finishGame) {
    case lidraughts.game.actorApi.FinishGame(game, _, _) =>
      if (game.hasCorrespondenceClock && !game.hasAi) coll.remove($id(game.id))
  }

  system.lidraughtsBus.subscribeFun('moveEventCorres) {
    case lidraughts.hub.actorApi.round.CorresMoveEvent(move, _, _, alarmable, _) if alarmable =>
      proxyGame(move.gameId) flatMap {
        _ ?? { game =>
          game.bothPlayersHaveMoved ?? {
            game.playableCorrespondenceClock ?? { clock =>
              val remainingTime = clock remainingTime game.turnColor
              val ringsAt = DateTime.now.plusSeconds(remainingTime.toInt * 8 / 10)
              coll.update(
                $id(game.id),
                Alarm(
                  _id = game.id,
                  ringsAt = ringsAt,
                  expiresAt = DateTime.now.plusSeconds(remainingTime.toInt * 2)
                ),
                upsert = true
              ).void
            }
          }
        }
      }
  }

  private def run: Unit = coll.find($doc(
    "ringsAt" $lt DateTime.now
  )).cursor[Alarm](ReadPreference.secondaryPreferred)
    .enumerator(100, Cursor.ContOnError())
    .|>>>(Iteratee.foldM[Alarm, Int](0) {
      case (count, alarm) => proxyGame(alarm._id).flatMap {
        _ ?? { game =>
          val pov = Pov(game, game.turnColor)
          socketMap.ask[Boolean](pov.gameId)(IsOnGame(pov.color, _)) addEffect {
            case true => // already looking at the game
            case false => system.lidraughtsBus.publish(
              lidraughts.game.actorApi.CorresAlarmEvent(pov),
              'corresAlarm
            )
          }
        }
      } >> coll.remove($id(alarm._id)) inject (count + 1)
    })
    .mon(_.round.alarm.time)
    .addEffect(c => lidraughts.mon.round.alarm.count(c))
    .addEffectAnyway(scheduleNext)
}
