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

  override def preStart() {
    scheduleNext
    context setReceiveTimeout 30.seconds
  }

  def scheduler = context.system.scheduler

  def scheduleNext = scheduler.scheduleOnce(10 seconds, self, Run)

  def receive = {

    case ReceiveTimeout =>
      val msg = "Titivate timed out!"
      logger.error(msg)
      throw new RuntimeException(msg)

    case Run =>
      $enumerate.over[Game]($query(Query.checkable), 5000) { game =>

        if (game.finished || game.isPgnImport)
          GameRepo unsetCheckAt game

        else if (game.outoftime(_ => chess.Clock.maxGraceMillis)) fuccess {
          roundMap ! Tell(game.id, Outoftime)
        }

        else if (game.abandoned) fuccess {
          roundMap ! Tell(game.id, Abandon)
        }

        else if (game.unplayed) {
          bookmark ! lila.hub.actorApi.bookmark.Remove(game.id)
          GameRepo remove game.id
        }

        else game.clock match {

          case Some(clock) if clock.isRunning =>
            val minutes = (clock.estimateTotalTime / 60).toInt
            GameRepo.setCheckAt(game, DateTime.now plusMinutes minutes)

          case Some(clock) =>
            val hours = Game.unplayedHours
            GameRepo.setCheckAt(game, DateTime.now plusHours hours)

          case None =>
            val days = game.daysPerTurn | game.hasAi.fold(Game.aiAbandonedDays, Game.abandonedDays)
            GameRepo.setCheckAt(game, DateTime.now plusDays days)
        }
      } andThenAnyway scheduleNext
  }
}
