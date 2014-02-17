package lila.round

import chess.Color._
import chess.Status._
import chess.{ Status, Color, Speed, Variant }

import lila.db.api._
import lila.game.actorApi.FinishGame
import lila.game.tube.gameTube
import lila.game.{ GameRepo, Game, Pov, Event }
import lila.i18n.I18nKey.{ Select => SelectI18nKey }
import lila.user.tube.userTube
import lila.user.{ User, UserRepo }

private[round] final class Finisher(
    messenger: Messenger,
    perfsUpdater: PerfsUpdater,
    bus: lila.common.Bus) {

  def apply(
    game: Game,
    status: Status.type => Status,
    winner: Option[Color] = None,
    message: Option[SelectI18nKey] = None): Fu[Events] = {
    val prog = game.finish(status(Status), winner)
    val g = prog.game
    (GameRepo save prog) >>
      GameRepo.finish(g.id, winner, winner flatMap (g.player(_).userId)) >>
      UserRepo.pair(
        g.player(White).userId,
        g.player(Black).userId).flatMap {
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

  private def updateCountAndPerfs(finish: FinishGame): Funit =
    (finish.white |@| finish.black).tupled ?? {
      case (white, black) => perfsUpdater.save(finish.game, white, black)
    } zip
      (finish.white ?? incNbGames(finish.game)) zip
      (finish.black ?? incNbGames(finish.game)) void

  private def incNbGames(game: Game)(user: User): Funit = game.finished ?? {
    UserRepo.incNbGames(user.id, game.rated, game.hasAi,
      result = if (game.winnerUserId exists (user.id==)) 1
      else if (game.loserUserId exists (user.id==)) -1
      else 0)
  }
}
