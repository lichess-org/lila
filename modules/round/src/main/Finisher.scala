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
    aiPerfApi: lila.ai.AiPerfApi,
    crosstableApi: lila.game.CrosstableApi,
    reminder: Reminder,
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
        } >>- (reminder remind g)
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

  private def addAiGame(finish: FinishGame): Funit = ~{
    import finish._
    import lila.rating.Glicko.Result._
    for {
      level <- game.players.map(_.aiLevel).flatten.headOption
      if game.turns > 10
      if !game.fromPosition
      humanColor <- game.players.find(_.isHuman).map(_.color)
      user <- humanColor.fold(white, black)
      if !user.engine
      result = game.winnerColor match {
        case Some(c) if c == humanColor => Loss
        case Some(_)                    => Win
        case None                       => Draw
      }
    } yield aiPerfApi.add(level, user.perfs.global, result)
  }

  private def incNbGames(game: Game)(user: User): Funit = game.finished ?? {
    UserRepo.incNbGames(user.id, game.rated, game.hasAi,
      result = if (game.winnerUserId exists (user.id==)) 1
      else if (game.loserUserId exists (user.id==)) -1
      else 0)
  }
}
