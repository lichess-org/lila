package lila.round

import akka.actor._
import akka.stream.scaladsl._
import lila.db.dsl._
import lila.game.{ Query, Game, GameRepo }
import lila.hub.DuctMap
import lila.round.actorApi.round.{ QuietFlag, Abandon }
import org.joda.time.DateTime
import org.reactivestreams.Publisher
import reactivemongo.akkastream.{ AkkaStreamCursor, cursorProducer, State }
import reactivemongo.api._
import scala.concurrent.duration._

/*
 * Cleans up unfinished games
 * and flagged games when no one is around
 */
private[round] final class Titivate(
    tellRound: TellRound,
    gameRepo: GameRepo,
    bookmark: lila.hub.actors.Bookmark,
    chatApi: lila.chat.ChatApi
)(implicit mat: akka.stream.Materializer) extends Actor {

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

    case Run => gameRepo.count(_.checkable).flatMap { total =>
      lila.mon.round.titivate.total(total)
      gameRepo.cursor(Query.checkable).documentSource()
        .via(Flow[Game] take 100).via(gameFlow).toMat(gameSink)(Keep.right).run
        .mon(_.round.titivate.time)
        .addEffect(lila.mon.round.titivate.game(_))
        .>> {
          gameRepo.count(_.checkableOld).map(lila.mon.round.titivate.old(_))
        }
        .addEffectAnyway(scheduleNext)
    }
  }

  private lazy val gameSink: Sink[Unit, Fu[Int]] = Sink.fold[Int, Unit](0)((acc, _) => acc + 1)

  private lazy val gameFlow: Flow[Game, Unit, _] = Flow[Game].mapAsyncUnordered(8) {

    case game if game.finished || game.isPgnImport || game.playedThenAborted =>
      gameRepo unsetCheckAt game void

    case game if game.outoftime(withGrace = true) => fuccess {
      tellRound(game.id, QuietFlag)
    }

    case game if game.abandoned => fuccess {
      tellRound(game.id, Abandon)
    }

    case game if game.unplayed =>
      bookmark ! lila.hub.actorApi.bookmark.Remove(game.id)
      chatApi.remove(lila.chat.Chat.Id(game.id))
      gameRepo remove game.id

    case game => game.clock match {

      case Some(clock) if clock.isRunning =>
        val minutes = clock.estimateTotalSeconds / 60
        gameRepo.setCheckAt(game, DateTime.now plusMinutes minutes).void

      case Some(clock) =>
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
