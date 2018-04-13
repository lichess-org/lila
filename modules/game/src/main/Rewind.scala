package lila.game

import org.joda.time.DateTime
import scalaz.Validation.FlatMap._

import chess.format.{ pgn => chessPgn }

object Rewind {

  private def createTags(fen: Option[String], game: Game) = {
    val variantTag = Some(chessPgn.Tag(_.Variant, game.variant.name))
    val fenTag = fen map (fenString => chessPgn.Tag(_.FEN, fenString))

    chessPgn.Tags(List(variantTag, fenTag).flatten)
  }

  def apply(game: Game, initialFen: Option[String]): Valid[Progress] = chessPgn.Reader.movesWithSans(
    moveStrs = game.pgnMoves,
    op = sans => chessPgn.Sans(sans.value.dropRight(1)),
    tags = createTags(initialFen, game)
  ).flatMap(_.valid) map { replay =>
      val rewindedGame = replay.state
      val rewindedHistory = rewindedGame.board.history
      val rewindedSituation = rewindedGame.situation
      val color = game.turnColor;
      val newClock = game.clock.map(_.takeback) map { clk =>
        game.clockHistory.flatMap(_.last(color)).fold(clk) {
          t => clk.setRemainingTime(color, t)
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
