package lila.round

import akka.actor._
import akka.stream.scaladsl._
import org.joda.time.DateTime
import reactivemongo.api._
import scala.concurrent.duration._

import lila.db.dsl._
import lila.game.{ Game, GameRepo, Query }
import lila.round.actorApi.round.{ Abandon, QuietFlag }
import lila.common.LilaStream

/*
 * Cleans up unfinished games
 * and flagged games when no one is around
 */
final private[round] class Titivate(
    tellRound: TellRound,
    gameRepo: GameRepo,
    bookmark: lila.hub.actors.Bookmark,
    chatApi: lila.chat.ChatApi
)(implicit mat: akka.stream.Materializer)
    extends Actor {

  object Run

  override def preStart(): Unit = {
    scheduleNext
    context setReceiveTimeout 30.seconds
  }

  def scheduler = context.system.scheduler

  def scheduleNext = scheduler.scheduleOnce(5 seconds, self, Run)

  def receive = {
    case ReceiveTimeout =>
      val msg = "Titivate timed out!"
      logger.error(msg)
      throw new RuntimeException(msg)

    case Run =>
      gameRepo.count(_.checkable).flatMap { total =>
        lila.mon.round.titivate.total.record(total)
        gameRepo
          .cursor(Query.checkable)
          .documentSource()
          .take(100)
          .via(gameFlow)
          .toMat(LilaStream.sinkCount)(Keep.right)
          .run
          .addEffect(lila.mon.round.titivate.game.record(_))
          .>> {
            gameRepo
              .count(_.checkableOld)
              .dmap(lila.mon.round.titivate.old.record(_))
          }
          .monSuccess(_.round.titivate.time)
          .addEffectAnyway(scheduleNext)
      }
  }

  private lazy val gameFlow: Flow[Game, Unit, _] = Flow[Game].mapAsyncUnordered(8) {

    case game if game.finished || game.isPgnImport || game.playedThenAborted =>
      gameRepo unsetCheckAt game void

    case game if game.outoftime(withGrace = true) =>
      fuccess {
        tellRound(game.id, QuietFlag)
      }

    case game if game.abandoned =>
      fuccess {
        tellRound(game.id, Abandon)
      }

    case game if game.unplayed =>
      bookmark ! lila.hub.actorApi.bookmark.Remove(game.id)
      chatApi.remove(lila.chat.Chat.Id(game.id))
      gameRepo remove game.id

    case game =>
      game.clock match {

        case Some(clock) if clock.isRunning =>
          val minutes = clock.estimateTotalSeconds / 60
          gameRepo.setCheckAt(game, DateTime.now plusMinutes minutes).void

        case Some(_) =>
          val hours = Game.unplayedHours
          gameRepo.setCheckAt(game, DateTime.now plusHours hours).void

        case None =>
          val hours = game.daysPerTurn.fold(
            if (game.hasAi) Game.aiAbandonedHours
            else Game.abandonedDays * 24
          )(_ * 24)
          gameRepo.setCheckAt(game, DateTime.now plusHours hours).void
      }
  }
}
