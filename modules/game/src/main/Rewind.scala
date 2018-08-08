package lidraughts.game

import org.joda.time.DateTime
import scalaz.Validation.FlatMap._

import draughts.format.{ pdn => draughtsPdn }

object Rewind {

  private def createTags(fen: Option[String], game: Game) = {
    val variantTag = Some(draughtsPdn.Tag(_.GameType, game.variant.gameType))
    val fenTag = fen map (fenString => draughtsPdn.Tag(_.FEN, fenString))

    draughtsPdn.Tags(List(variantTag, fenTag).flatten)
  }

  def apply(game: Game, initialFen: Option[String]): Valid[Progress] = {
    logger.info(s"Rewind.apply game: $game")
    draughtsPdn.Reader.movesWithSans(
      moveStrs = game.pdnMoves,
      op = sans => draughtsPdn.Sans(sans.value.dropRight(1)),
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
          draughts = rewindedGame.copy(clock = newClock),
          binaryMoveTimes = game.binaryMoveTimes.map { binary =>
            val moveTimes = BinaryFormat.moveTime.read(binary, game.playedTurns)
            BinaryFormat.moveTime.write(moveTimes.dropRight(1))
          },
          clockHistory = game.clockHistory.map(_.update(!color, _.dropRight(1))),
          movedAt = DateTime.now
        )
        Progress(game, newGame)
      }
  }
}
