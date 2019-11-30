package lila.round

import org.joda.time.DateTime
import play.api.libs.iteratee._
import reactivemongo.api._
import reactivemongo.play.iteratees.cursorProducer
import scala.concurrent.duration._

import lila.common.{ Bus, Tellable }
import lila.db.dsl._
import lila.game.{ Game, Pov }
import lila.hub.actorApi.round.IsOnGame
import makeTimeout.short

private final class CorresAlarm(
    system: akka.actor.ActorSystem,
    coll: Coll,
    hasUserId: (Game, lila.user.User.ID) => Fu[Boolean],
    proxyGame: Game.ID => Fu[Option[Game]]
) {

  private case class Alarm(
      _id: String, // game id
      ringsAt: DateTime, // when to notify the player
      expiresAt: DateTime
  )

  private implicit val AlarmHandler = reactivemongo.api.bson.Macros.handler[Alarm]

  private def scheduleNext: Unit = system.scheduler.scheduleOnce(10 seconds)(run)

  system.scheduler.scheduleOnce(10 seconds)(scheduleNext)

  Bus.subscribeFun("finishGame") {
    case lila.game.actorApi.FinishGame(game, _, _) =>
      if (game.hasCorrespondenceClock && !game.hasAi) coll.remove($id(game.id))
  }

  Bus.subscribeFun("moveEventCorres") {
    case lila.hub.actorApi.round.CorresMoveEvent(move, _, _, alarmable, _) if alarmable =>
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
          pov.player.userId.fold(fuccess(true))(u => hasUserId(pov.game, u)) addEffect {
            case true => // already looking at the game
            case false => Bus.publish(
              lila.game.actorApi.CorresAlarmEvent(pov),
              "corresAlarm"
            )
          }
        }
      } >> coll.remove($id(alarm._id)) inject (count + 1)
    })
    .mon(_.round.alarm.time)
    .addEffect(c => lila.mon.round.alarm.count(c))
    .addEffectAnyway(scheduleNext)
}
