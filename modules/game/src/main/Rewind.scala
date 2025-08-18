package lila.game

import chess.ErrorStr
import chess.format.Fen
import monocle.syntax.all.*

object Rewind:

  def apply(game: CoreGame, initialFen: Option[Fen.Full]): Either[ErrorStr, Progress] =
    chess
      .Game(game.variant, initialFen)
      .forward(game.sans.dropRight(1))
      .map: rewindedGame =>
        val color = game.turnColor
        val newClock = game.clock.map(_.takeback).map { clk =>
          clk.updatePlayer(color): clkPlayer =>
            clkPlayer.setRemaining(game.clockHistory.flatMap(_(color).lastOption) | clkPlayer.limit)
        }
        val newGame = game.copy(
          players = game.players.map(_.removeTakebackProposition),
          chess = rewindedGame.copy(clock = newClock),
          binaryMoveTimes = game.binaryMoveTimes.map { binary =>
            val moveTimes = BinaryFormat.moveTime.read(binary, game.playedPlies)
            BinaryFormat.moveTime.write(moveTimes.dropRight(1))
          },
          loadClockHistory = _ => game.clockHistory.map(_.update(!color, _.dropRight(1))),
          movedAt = nowInstant,
          metadata = game.metadata.focus(_.drawOffers).modify(_.beforePly(rewindedGame.ply))
        )
        Progress(game, newGame)
