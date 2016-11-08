package lila.round

import akka.actor._
import org.joda.time.DateTime
import play.api.libs.iteratee._
import reactivemongo.api._
import scala.concurrent.duration._

import lila.db.dsl._
import lila.game.GameRepo

private final class CorresAlarm(coll: Coll) extends Actor {

  object Schedule
  object Run

  case class Alarm(
    _id: String, // game id
    ringsAt: DateTime, // when to notify the player
    expiresAt: DateTime)

  private implicit val AlarmHandler = reactivemongo.bson.Macros.handler[Alarm]

  override def preStart() {
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
          case (count, alarm) => {
            context.system.lilaBus.publish(
              lila.hub.actorApi.round.CorresAlarmEvent(alarm._id),
              'corresAlarm)
            coll.remove($id(alarm._id))
          } inject (count + 1)
        })
        .chronometer.mon(_.round.alarm.time).result
        .addEffect(c => lila.mon.round.alarm.count(c))
        .andThenAnyway(scheduleNext)

    case lila.game.actorApi.FinishGame(game, _, _) =>
      if (game.hasCorrespondenceClock && !game.hasAi) coll.remove($id(game.id))

    case move: lila.hub.actorApi.round.MoveEvent if move.alarmable =>
      GameRepo game move.gameId flatMap {
        _ ?? { game =>
          game.playableCorrespondenceClock ?? { clock =>
            val remainingTime = clock remainingTime game.turnColor
            val ringsAt = DateTime.now.plusSeconds(remainingTime.toInt * 9 / 10)
            // val ringsAt = DateTime.now.plusSeconds(5)
            coll.update(
              $id(game.id),
              Alarm(
                _id = game.id,
                ringsAt = ringsAt,
                expiresAt = DateTime.now.plusSeconds(remainingTime.toInt * 2)),
              upsert = true).void
          }
        }
      }
  }
}
