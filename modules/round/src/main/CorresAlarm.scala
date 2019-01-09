package lila.round

import org.joda.time.DateTime
import play.api.libs.iteratee._
import reactivemongo.api._
import reactivemongo.play.iteratees.cursorProducer
import scala.concurrent.duration._

import lila.common.Tellable
import lila.db.dsl._
import lila.game.{ GameRepo, Pov }
import lila.hub.actorApi.round.IsOnGame
import makeTimeout.short

private final class CorresAlarm(
    system: akka.actor.ActorSystem,
    coll: Coll,
    socketMap: SocketMap
) {

  private case class Alarm(
      _id: String, // game id
      ringsAt: DateTime, // when to notify the player
      expiresAt: DateTime
  )

  private implicit val AlarmHandler = reactivemongo.bson.Macros.handler[Alarm]

  private def scheduleNext: Unit = system.scheduler.scheduleOnce(10 seconds)(run)

  system.scheduler.scheduleOnce(10 seconds)(scheduleNext)

  system.lilaBus.subscribeFun('finishGame) {
    case lila.game.actorApi.FinishGame(game, _, _) =>
      if (game.hasCorrespondenceClock && !game.hasAi) coll.remove($id(game.id))
  }

  system.lilaBus.subscribeFun('moveEventCorres) {
    case lila.hub.actorApi.round.CorresMoveEvent(move, _, _, alarmable, _) if alarmable =>
      GameRepo game move.gameId flatMap {
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
      case (count, alarm) => GameRepo.game(alarm._id).flatMap {
        _ ?? { game =>
          val pov = Pov(game, game.turnColor)
          socketMap.ask[Boolean](pov.gameId)(IsOnGame(pov.color, _)) addEffect {
            case true => // already looking at the game
            case false => system.lilaBus.publish(
              lila.game.actorApi.CorresAlarmEvent(pov),
              'corresAlarm
            )
          }
        }
      } >> coll.remove($id(alarm._id)) inject (count + 1)
    })
    .mon(_.round.alarm.time)
    .addEffect(c => lila.mon.round.alarm.count(c))
    .addEffectAnyway(scheduleNext)
}
