package lila.round

import akka.actor.*
import akka.stream.scaladsl.*

import lila.common.LilaStream
import lila.core.round.{ Abandon, QuietFlag }
import lila.db.dsl.*
import lila.game.GameExt.abandoned
import lila.game.{ GameRepo, Query }

/*
 * Cleans up unfinished games
 * and flagged games when no one is around
 */
final private class Titivate(
    roundApi: lila.core.round.RoundApi,
    gameRepo: GameRepo,
    chatApi: lila.chat.ChatApi
)(using akka.stream.Materializer)
    extends Actor:

  private type GameOrFail = Either[(GameId, Throwable), Game]

  object Run

  override def preStart(): Unit =
    scheduleNext()
    context.setReceiveTimeout(30.seconds)

  given Executor = context.system.dispatcher

  def scheduleNext(): Unit = context.system.scheduler.scheduleOnce(5.seconds, self, Run)

  def receive =
    case ReceiveTimeout =>
      val msg = "Titivate timed out!"
      logBranch.error(msg)
      throw new RuntimeException(msg)

    case Run =>
      gameRepo
        .countSec(_.checkable)
        .foreach: total =>
          lila.mon.round.titivate.total.record(total)
          gameRepo
            .docCursor(Query.checkable)
            .documentSource(100)
            .via(gameRead)
            .via(gameFlow)
            .toMat(LilaStream.sinkCount)(Keep.right)
            .run()
            .addEffect(lila.mon.round.titivate.game.record(_))
            .>> {
              gameRepo
                .countSec(_.checkableOld)
                .dmap(lila.mon.round.titivate.old.record(_))
            }
            .monSuccess(_.round.titivate.time)
            .logFailure(logBranch)
            .addEffectAnyway(scheduleNext())

  private val logBranch = logger.branch("titivate")

  private val gameRead = Flow[Bdoc].map: doc =>
    gameRepo.gameHandler
      .readDocument(doc)
      .fold[GameOrFail](
        err => Left(GameId(~doc.string("_id")) -> err),
        Right.apply
      )

  private val unplayedHours     = 24
  private def unplayedDate      = nowInstant.minusHours(unplayedHours)
  private def unplayed(g: Game) = !g.bothPlayersHaveMoved && (g.createdAt.isBefore(unplayedDate))

  private val gameFlow: Flow[GameOrFail, Unit, ?] = Flow[GameOrFail].mapAsyncUnordered(8):

    case Left((id, err)) =>
      lila.mon.round.titivate.broken(err.getClass.getSimpleName).increment()
      logBranch.warn(s"Can't read game $id", err)
      gameRepo.unsetCheckAt(id)

    case Right(game) =>
      game match

        case game if game.finished || game.isPgnImport || (game.aborted && game.bothPlayersHaveMoved) =>
          gameRepo.unsetCheckAt(game.id)

        case game if game.outoftime(withGrace = true) =>
          fuccess:
            roundApi.tell(game.id, QuietFlag)

        case game if game.abandoned =>
          fuccess:
            roundApi.tell(game.id, Abandon)

        case game if unplayed(game) =>
          lila.common.Bus.publish(lila.core.round.DeleteUnplayed(game.id), "roundUnplayed")
          chatApi.remove(game.id.into(ChatId))
          gameRepo.remove(game.id)

        case game =>
          game.clock match

            case Some(clock) if clock.isRunning =>
              val minutes = clock.estimateTotalSeconds / 60
              gameRepo.setCheckAt(game, nowInstant.plusMinutes(minutes)).void

            case Some(_) =>
              val hours = unplayedHours
              gameRepo.setCheckAt(game, nowInstant.plusHours(hours)).void

            case None =>
              val days = game.daysPerTurn | lila.game.Game.abandonedDays
              gameRepo.setCheckAt(game, nowInstant.plusDays(days.value)).void
