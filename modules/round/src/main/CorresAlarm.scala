package lila.round

import akka.actor._
import akka.pattern.ask
import org.joda.time.DateTime
import play.api.libs.iteratee._
import reactivemongo.api._
import scala.concurrent.duration._

import lila.db.dsl._
import lila.hub.actorApi.map.Ask
import lila.hub.actorApi.round.IsOnGame
import lila.game.{ GameRepo, Pov }
import makeTimeout.short

private final class CorresAlarm(
    coll: Coll,
    roundSocketHub: ActorSelection
) extends Actor {

  object Schedule
  object Run

  case class Alarm(
      _id: String, // game id
      ringsAt: DateTime, // when to notify the player
      expiresAt: DateTime
  )

  private implicit val AlarmHandler = reactivemongo.bson.Macros.handler[Alarm]

  override def preStart(): Unit = {
    scheduleNext
    context setReceiveTimeout 1.minute
  }

  def scheduler = context.system.scheduler

  def scheduleNext = scheduler.scheduleOnce(10 seconds, self, Run)

  import reactivemongo.play.iteratees.cursorProducer

  def receive = {

    case ReceiveTimeout =>
      val msg = "CorresAlarm timed out!"
      logger.error(msg)
      throw new RuntimeException(msg)

    case Run =>
      coll.find($doc(
        "ringsAt" $lt DateTime.now
      )).cursor[Alarm](ReadPreference.secondaryPreferred)
        .enumerator(100, Cursor.ContOnError())
        .|>>>(Iteratee.foldM[Alarm, Int](0) {
          case (count, alarm) => GameRepo.game(alarm._id).flatMap {
            _ ?? { game =>
              val pov = Pov(game, game.turnColor)
              roundSocketHub ? Ask(pov.gameId, IsOnGame(pov.color)) mapTo manifest[Boolean] addEffect {
                case true => // already looking at the game
                case false => context.system.lilaBus.publish(
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

    case lila.game.actorApi.FinishGame(game, _, _) =>
      if (game.hasCorrespondenceClock && !game.hasAi) coll.remove($id(game.id))

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
}
