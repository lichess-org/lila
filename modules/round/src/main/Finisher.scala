package lila.round

import chess.Color._
import chess.Status._
import chess.{ Status, Color, Speed }

import lila.db.api._
import lila.game.actorApi.FinishGame
import lila.game.tube.gameTube
import lila.game.{ GameRepo, Game, Pov, Event }
import lila.i18n.I18nKey.{ Select => SelectI18nKey }
import lila.user.tube.userTube
import lila.user.{ User, UserRepo, Perfs }

private[round] final class Finisher(
    messenger: Messenger,
    perfsUpdater: PerfsUpdater,
    aiPerfApi: lila.ai.AiPerfApi,
    crosstableApi: lila.game.CrosstableApi,
    bus: lila.common.Bus,
    casualOnly: Boolean) {

  def apply(
    game: Game,
    status: Status.type => Status,
    winner: Option[Color] = None,
    message: Option[SelectI18nKey] = None): Fu[Events] = {
    val prog = game.finish(status(Status), winner)
    casualOnly.fold(
      GameRepo unrate prog.game.id inject prog.game.copy(mode = chess.Mode.Casual),
      fuccess(prog.game)
    ) flatMap { g =>
        (GameRepo save prog) >>
          GameRepo.finish(g.id, winner, winner flatMap (g.player(_).userId)) >>
          UserRepo.pair(
            g.whitePlayer.userId,
            g.blackPlayer.userId).flatMap {
              case (whiteO, blackO) => {
                val finish = FinishGame(g, whiteO, blackO)
                updateCountAndPerfs(finish) inject {
                  message foreach { messenger.system(g, _) }
                  bus.publish(finish, 'finishGame)
                  prog.events
                }
              }
            }
      }
  }

  private def updateCountAndPerfs(finish: FinishGame): Funit =
    (!finish.isVsSelf && !finish.game.aborted) ?? {
      (finish.white |@| finish.black).tupled ?? {
        case (white, black) =>
          crosstableApi add finish.game zip perfsUpdater.save(finish.game, white, black)
      } zip
        addAiGame(finish) zip
        (finish.white ?? incNbGames(finish.game)) zip
        (finish.black ?? incNbGames(finish.game)) void
    }

  private def addAiGame(finish: FinishGame): Funit =
    Perfs.variantLens(finish.game.variant) ?? { perfLens =>
      ~{
        import finish._
        import lila.rating.Glicko.Result._
        for {
          level <- game.players.map(_.aiLevel).flatten.headOption
          if game.turns > 10
          humanColor <- game.players.find(_.isHuman).map(_.color)
          user <- humanColor.fold(white, black)
          if !user.engine
          result = game.winnerColor match {
            case Some(c) if c == humanColor => Loss
            case Some(_)                    => Win
            case None                       => Draw
          }
        } yield aiPerfApi.add(level, perfLens(user.perfs), result)
      }
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
