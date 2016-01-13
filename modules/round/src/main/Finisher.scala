package lila.round

import chess.Color._
import chess.Status._
import chess.{ Status, Color, Speed }

import lila.db.api._
import lila.game.actorApi.{ FinishGame, AbortedBy }
import lila.game.{ GameRepo, Game, Pov, Event }
import lila.i18n.I18nKey.{ Select => SelectI18nKey }
import lila.playban.{ PlaybanApi, Outcome }
import lila.user.tube.userTube
import lila.user.{ User, UserRepo, Perfs }

private[round] final class Finisher(
    messenger: Messenger,
    perfsUpdater: PerfsUpdater,
    playban: PlaybanApi,
    aiPerfApi: lila.ai.AiPerfApi,
    crosstableApi: lila.game.CrosstableApi,
    bus: lila.common.Bus,
    timeline: akka.actor.ActorSelection,
    casualOnly: Boolean) {

  def abort(pov: Pov): Fu[Events] = apply(pov.game, _.Aborted) >>- {
    playban.abort(pov)
    bus.publish(AbortedBy(pov), 'abortGame)
  }

  def rageQuit(game: Game, winner: Option[Color]): Fu[Events] =
    apply(game, _.Timeout, winner) >>- winner.?? { color => playban.rageQuit(game, !color) }

  def other(
    game: Game,
    status: Status.type => Status,
    winner: Option[Color] = None,
    message: Option[SelectI18nKey] = None): Fu[Events] =
    apply(game, status, winner, message) >>- playban.goodFinish(game)

  private def apply(
    game: Game,
    status: Status.type => Status,
    winner: Option[Color] = None,
    message: Option[SelectI18nKey] = None): Fu[Events] = {
    val prog = game.finish(status(Status), winner)
    if (game.nonAi && game.isCorrespondence)
      Color.all foreach notifyTimeline(prog.game)
    casualOnly.fold(
      GameRepo unrate prog.game.id inject prog.game.copy(mode = chess.Mode.Casual),
      fuccess(prog.game)
    ) flatMap { g =>
        (GameRepo save prog) >>
          GameRepo.finish(
            id = g.id,
            winnerColor = winner,
            winnerId = winner flatMap (g.player(_).userId),
            status = prog.game.status) >>
            UserRepo.pair(
              g.whitePlayer.userId,
              g.blackPlayer.userId).flatMap {
                case (whiteO, blackO) => {
                  val finish = FinishGame(g, whiteO, blackO)
                  updateCountAndPerfs(finish) inject {
                    message foreach { messenger.system(g, _) }
                    GameRepo game g.id foreach { newGame =>
                      bus.publish(finish.copy(game = newGame | g), 'finishGame)
                    }
                    prog.events
                  }
                }
              }
      }
  }

  private def notifyTimeline(game: Game)(color: Color) = {
    import lila.hub.actorApi.timeline.{ Propagate, GameEnd }
    if (!game.aborted) game.player(color).userId foreach { userId =>
      game.perfType foreach { perfType =>
        timeline ! (Propagate(GameEnd(
          playerId = game fullIdOf color,
          opponent = game.player(!color).userId,
          win = game.winnerColor map (color ==),
          perf = perfType.key)) toUser userId)
      }
    }
  }

  private def updateCountAndPerfs(finish: FinishGame): Funit =
    (!finish.isVsSelf && !finish.game.aborted) ?? {
      (finish.white |@| finish.black).tupled ?? {
        case (white, black) =>
          crosstableApi add finish.game zip perfsUpdater.save(finish.game, white, black)
      } zip
        (finish.white ?? incNbGames(finish.game)) zip
        (finish.black ?? incNbGames(finish.game)) void
    }

  private def incNbGames(game: Game)(user: User): Funit = game.finished ?? {
    val totalTime = user.playTime.isDefined option game.moveTimes.sum / 10
    val tvTime = totalTime ifTrue game.metadata.tvAt.isDefined
    UserRepo.incNbGames(user.id, game.rated, game.hasAi,
      result = if (game.winnerUserId exists (user.id==)) 1
      else if (game.loserUserId exists (user.id==)) -1
      else 0,
      totalTime = totalTime,
      tvTime = tvTime)
  }
}
