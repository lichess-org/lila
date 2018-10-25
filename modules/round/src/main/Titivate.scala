package lila.round

import akka.actor._
import org.joda.time.DateTime
import play.api.libs.iteratee._
import reactivemongo.api._
import scala.concurrent.duration._

import lila.db.dsl._
import lila.game.{ Query, Game, GameRepo }
import lila.hub.DuctMap
import lila.round.actorApi.round.{ QuietFlag, Abandon }

/*
 * Cleans up unfinished games
 * and flagged games when no one is around
 */
private[round] final class Titivate(
    roundMap: DuctMap[Round],
    bookmark: ActorSelection,
    chat: ActorSelection
) extends Actor {

  object Run

  override def preStart(): Unit = {
    scheduleNext
    context setReceiveTimeout 30.seconds
  }

  def scheduler = context.system.scheduler

  def scheduleNext = scheduler.scheduleOnce(5 seconds, self, Run)

  import reactivemongo.play.iteratees.cursorProducer

  def receive = {
    case ReceiveTimeout =>
      val msg = "Titivate timed out!"
      logger.error(msg)
      throw new RuntimeException(msg)

    case Run => GameRepo.count(_.checkable).flatMap { total =>
      GameRepo.cursor(Query.checkable)
        .enumerator(1000, Cursor.ContOnError())
        .|>>>(Iteratee.foldM[Game, Int](0) {
          case (count, game) => {

            if (game.finished || game.isPgnImport || game.playedThenAborted)
              GameRepo unsetCheckAt game

            else if (game.outoftime(withGrace = true)) fuccess {
              roundMap.tell(game.id, QuietFlag)
            }

            else if (game.abandoned) fuccess {
              roundMap.tell(game.id, Abandon)
            }

            else if (game.unplayed) {
              bookmark ! lila.hub.actorApi.bookmark.Remove(game.id)
              chat ! lila.chat.actorApi.Remove(lila.chat.Chat.Id(game.id))
              GameRepo remove game.id
            } else game.clock match {

              case Some(clock) if clock.isRunning =>
                val minutes = clock.estimateTotalSeconds / 60
                GameRepo.setCheckAt(game, DateTime.now plusMinutes minutes)

              case Some(clock) =>
                val hours = Game.unplayedHours
                GameRepo.setCheckAt(game, DateTime.now plusHours hours)

              case None =>
                val hours = game.daysPerTurn.fold(
                  if (game.hasAi) Game.aiAbandonedHours
                  else Game.abandonedDays * 24
                )(_ * 24)
                GameRepo.setCheckAt(game, DateTime.now plusHours hours)
            }
          } inject (count + 1)
        })
        .mon(_.round.titivate.time)
        .addEffect { count =>
          lila.mon.round.titivate.game(count)
          lila.mon.round.titivate.total(total)
        }.>> {
          GameRepo.count(_.checkableOld).map(lila.mon.round.titivate.old(_))
        }
        .addEffectAnyway(scheduleNext)
    }
  }
}
