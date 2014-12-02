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

  def receive = {

    case Schedule =>
      scheduler.scheduleOnce(10 minutes, self, Run)

    case Run =>
      loginfo("run")
      var nb = 0
      $enumerate.over[Game]($query(Query.checkable), 5000) { game =>
        nb = nb + 1
        if (game.finished || game.isPgnImport) {
          loginfo(s"$nb ${game.id} unset")
          GameRepo unsetCheckAt game
        }
        else if (game.outoftimePlayer.isDefined) delay {
          loginfo(s"$nb ${game.id} outoftime")
          roundMap ! Tell(game.id, Outoftime)
        }
        else if (game.abandoned) delay {
          loginfo(s"$nb ${game.id} abandon")
          roundMap ! Tell(game.id, Abandon)
        }
        else if (game.unplayed) {
          loginfo(s"$nb ${game.id} unplayed")
          bookmark ! lila.hub.actorApi.bookmark.Remove(game.id)
          GameRepo remove game.id
        }
        else if (game.hasClock) {
          val hours = game.started.fold(1, 6)
          loginfo(s"$nb ${game.id} reschedule clock in $hours hours")
          GameRepo.setCheckAt(game, DateTime.now plusHours hours)
        }
        else {
          val days = game.daysPerTurn | 3
          loginfo(s"$nb ${game.id} reschedule slow in $days days")
          GameRepo.setCheckAt(game, DateTime.now plusDays days)
        }
      }.void andThenAnyway {
        self ! Schedule
      }
  }
}
