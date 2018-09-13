package lidraughts.draughtsnet

import org.joda.time.DateTime

import draughts.{ White, Black }
import draughts.format.{ FEN, Forsyth }

import lidraughts.game.{ Game, GameRepo, UciMemo }

final class Player(
    moveDb: MoveDB,
    uciMemo: UciMemo,
    val maxPlies: Int
) {

  def apply(game: Game): Funit = game.aiLevel ?? { level =>
    makeWork(game, level) addEffect moveDb.add void
  } recover {
    case e: Exception => logger.info(e.getMessage)
  }

  private def makeWork(game: Game, level: Int): Fu[Work.Move] =
    if (game.situation playable true)
      if (game.turns <= maxPlies) uciMemo.get(game) map { moves =>
        Work.Move(
          _id = Work.makeId,
          game = Work.Game(
            id = game.id,
            initialFen = FEN(Forsyth >> game.draughts).some, //pass current fen and no move history
            studyId = none,
            variant = game.variant,
            moves = Nil
          ),
          currentFen = FEN(Forsyth >> game.draughts),
          level = level,
          clock = game.clock.map { clk =>
            Work.Clock(
              wtime = clk.remainingTime(White).centis,
              btime = clk.remainingTime(Black).centis,
              inc = clk.incrementSeconds
            )
          },
          tries = 0,
          lastTryByKey = none,
          acquired = none,
          createdAt = DateTime.now
        )
      }
      else fufail(s"[draughtsnet] Too many moves (${game.turns}), won't play ${game.id}")
    else fufail(s"[draughtsnet] invalid position on ${game.id}")
}
