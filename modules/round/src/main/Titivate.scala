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

  def delay(f: => Unit): Funit = akka.pattern.after(50 millis, scheduler)(Future(f))

  def log(msg: String) {
    play.api.Logger.info(s"[titivate] $msg")
  }

  def receive = {

    case Schedule =>
      scheduler.scheduleOnce(10 seconds, self, Run)

    case Run =>
      log("run")
      $enumerate.over[Game]($query(Query.checkable), 5000) { game =>
        if (game.finished || game.isPgnImport) {
          log(s"${game.id} unset")
          GameRepo unsetCheckAt game
        }
        else if (game.outoftimePlayer.isDefined) delay {
          log(s"${game.id} outoftime")
          roundMap ! Tell(game.id, Outoftime)
        }
        else if (game.abandoned) delay {
          log(s"${game.id} abandon")
          roundMap ! Tell(game.id, Abandon)
        }
        else if (game.unplayed) {
          log(s"${game.id} unplayed")
          bookmark ! lila.hub.actorApi.bookmark.Remove(game.id)
          GameRepo remove game.id
        }
        else if (game.hasClock) {
          val hours = game.started.fold(1, 6)
          log(s"${game.id} reschedule clock in $hours hours")
          GameRepo.setCheckAt(game, DateTime.now plusHours hours)
        }
        else {
          val days = game.daysPerTurn | 3
          log(s"${game.id} reschedule slow in $days days")
          GameRepo.setCheckAt(game, DateTime.now plusDays days)
        }
      }.void andThenAnyway {
        self ! Schedule
      }
  }
}
