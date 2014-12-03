package lila.round

import akka.actor._
import org.joda.time.DateTime
import scala.concurrent.duration._
import scala.concurrent.Future

import lila.db.api._
import lila.game.tube.gameTube
import lila.game.{ Query, Game, GameRepo }
import lila.hub.actorApi.map.Tell
import lila.round.actorApi.round.{ Outoftime, Abandon }

private[round] final class Titivate(
    roundMap: ActorRef,
    bookmark: ActorSelection) extends Actor {

  object Schedule
  object Run

  def scheduler = context.system.scheduler

  override def preStart() {
    self ! Schedule
  }

  val delayDuration = 200 millis
  def delayF(f: => Funit): Funit = akka.pattern.after(delayDuration, scheduler)(f)
  def delay(f: => Unit): Funit = akka.pattern.after(delayDuration, scheduler)(Future(f))

  def receive = {

    case Schedule =>
      scheduler.scheduleOnce(30 seconds, self, Run)

    case Run =>
      $enumerate.over[Game]($query(Query.checkable), 5000) { game =>
        if (game.finished || game.isPgnImport) delayF {
          GameRepo unsetCheckAt game
        }
        else if (game.outoftimePlayer.isDefined) delay {
          roundMap ! Tell(game.id, Outoftime)
        }
        else if (game.abandoned) delay {
          roundMap ! Tell(game.id, Abandon)
        }
        else if (game.unplayed) delayF {
          bookmark ! lila.hub.actorApi.bookmark.Remove(game.id)
          GameRepo remove game.id
        }
        else game.clock match {
          case Some(clock) if clock.isRunning => delayF {
            val minutes = (clock.estimateTotalTime / 60).toInt
            GameRepo.setCheckAt(game, DateTime.now plusMinutes minutes)
          }
          case Some(clock) => delayF {
            val hours = Game.unplayedHours
            GameRepo.setCheckAt(game, DateTime.now plusHours hours)
          }
          case None => delayF {
            val days = game.daysPerTurn | Game.abandonedDays
            GameRepo.setCheckAt(game, DateTime.now plusDays days)
          }
        }
      }.void andThenAnyway {
        self ! Schedule
      }
  }
}
