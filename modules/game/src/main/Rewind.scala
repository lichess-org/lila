package lila.game

import chess.ErrorStr
import chess.format.{ Fen, pgn as chessPgn }

object Rewind:

  private def createTags(fen: Option[Fen.Full], game: CoreGame) =
    val variantTag = Some(chessPgn.Tag(_.Variant, game.variant.name))
    val fenTag     = fen.map(f => chessPgn.Tag(_.FEN, f.value))
    chessPgn.Tags(List(variantTag, fenTag).flatten)

  def apply(game: CoreGame, initialFen: Option[Fen.Full]): Either[ErrorStr, Progress] =
    chessPgn.Reader
      .movesWithSans(
        sans = game.sans,
        op = sans => chessPgn.Sans(sans.value.dropRight(1)),
        tags = createTags(initialFen, game)
      )
      .flatMap(_.valid)
      .map { replay =>
        val rewindedGame = replay.state
        val color        = game.turnColor
        val newClock = game.clock.map(_.takeback).map { clk =>
          game.clockHistory.flatMap(_.last(color)).fold(clk) { t =>
            clk.setRemainingTime(color, t)
          }
        }
        val newGame = game.copy(
          players = game.players.map(_.removeTakebackProposition),
          chess = rewindedGame.copy(clock = newClock),
          binaryMoveTimes = game.binaryMoveTimes.map { binary =>
            val moveTimes = BinaryFormat.moveTime.read(binary, game.playedTurns)
            BinaryFormat.moveTime.write(moveTimes.dropRight(1))
          },
          loadClockHistory = _ => game.clockHistory.map(_.update(!color, _.dropRight(1))),
          movedAt = nowInstant
        )
        Progress(game, newGame)
      }
