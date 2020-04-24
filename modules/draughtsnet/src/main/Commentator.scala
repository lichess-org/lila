package lidraughts.draughtsnet

import org.joda.time.DateTime

import draughts.format.{ FEN, Forsyth }
import lidraughts.game.{ Game, GameRepo, Rewind, UciMemo }
import lidraughts.hub.actorApi.draughtsnet.CommentaryEvent

final class Commentator(
    bus: lidraughts.common.Bus,
    commentDb: CommentDB,
    uciMemo: UciMemo,
    evalCacheApi: lidraughts.evalCache.EvalCacheApi,
    maxPlies: Int
) {

  def apply(game: Game): Funit =
    if (game.situation.checkMate || game.situation.ghosts != 0) funit
    else evalCacheApi.getSinglePvEval(
      game.variant,
      FEN(Forsyth >> game.situation)
    ) map {
        case Some(eval) =>
          bus.publish(
            CommentaryEvent(
              gameId = game.id,
              simulId = game.simulId,
              evalJson = lidraughts.evalCache.JsonHandlers.gameEvalJson(game.id, eval)
            ),
            'draughtsnetComment
          )
        case _ =>
          makeWork(game).addEffect(commentDb.add).void recover {
            case e: Exception => logger.info(e.getMessage)
          }
      }

  def apply(id: Game.ID): Funit =
    GameRepo.game(id) flatMap {
      case Some(game) => apply(game)
      case _ => funit
    }

  private def makeWork(game: Game): Fu[Work.Commentary] =
    if (game.situation.playable(true) && game.situation.ghosts == 0)
      if (game.turns <= maxPlies) GameRepo.initialFen(game) zip uciMemo.get(game) map {
        case (initialFen, moves) => Work.Commentary(
          _id = Work.makeId,
          game = Work.Game(
            id = game.id,
            initialFen = initialFen,
            studyId = none,
            simulId = game.simulId,
            variant = game.variant,
            moves = moves.toList
          ),
          currentFen = FEN(Forsyth >> game.draughts),
          tries = 0,
          lastTryByKey = none,
          acquired = none,
          createdAt = DateTime.now
        )
      }
      else fufail(s"[draughtsnet] Too many moves (${game.turns}), won't comment on ${game.id}")
    else fufail(s"[draughtsnet] invalid position on ${game.id} move ${game.turns}: ${game.situation}")
}
