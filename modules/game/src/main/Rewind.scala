package lila.game

import cats.data.Validated
import chess.format.{ FEN, pgn => chessPgn }
import org.joda.time.DateTime

object Rewind {

  private def createTags(fen: Option[FEN], game: Game) = {
    val variantTag = Some(chessPgn.Tag(_.Variant, game.variant.name))
    val fenTag     = fen.map(f => chessPgn.Tag(_.FEN, f.value))

    chessPgn.Tags(List(variantTag, fenTag).flatten)
  }

  def apply(game: Game, initialFen: Option[FEN]): Validated[String, Progress] =
    chessPgn.Reader
      .movesWithSans(
        moveStrs = game.pgnMoves,
        op = sans => chessPgn.Sans(sans.value.dropRight(1)),
        tags = createTags(initialFen, game)
      )
      .flatMap(_.valid) map { replay =>
      val rewindedGame = replay.state
      val color        = game.turnColor
      val newClock = game.clock.map(_.takeback) map { clk =>
        game.clockHistory.flatMap(_.last(color)).fold(clk) { t =>
          clk.setRemainingTime(color, t)
        }
      }
      def rewindPlayer(player: Player) = player.copy(proposeTakebackAt = 0)
      val newGame = game.copy(
        whitePlayer = rewindPlayer(game.whitePlayer),
        blackPlayer = rewindPlayer(game.blackPlayer),
        chess = rewindedGame.copy(clock = newClock),
        binaryMoveTimes = game.binaryMoveTimes.map { binary =>
          val moveTimes = BinaryFormat.moveTime.read(binary, game.playedTurns)
          BinaryFormat.moveTime.write(moveTimes.dropRight(1))
        },
        loadClockHistory = _ => game.clockHistory.map(_.update(!color, _.dropRight(1))),
        movedAt = DateTime.now
      )
      Progress(game, newGame)
    }
}
