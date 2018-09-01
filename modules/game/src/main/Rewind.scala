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

  def apply(game: Game, initialFen: Option[String], initialPly: Int, takeBacker: draughts.Color, chokeCapture: Boolean = false): Valid[Progress] = {
    draughtsPdn.Reader.movesWithSans(
      moveStrs = game.pdnMoves,
      op = sans => draughtsPdn.Sans(sans.value.dropRight(1)),
      tags = createTags(initialFen, game)
    ).flatMap(_.valid) map { replay =>
        val rewindedGame = replay.state
        val rewindedHistory = rewindedGame.board.history
        val rewindedSituation = rewindedGame.situation
        val color = game.turnColor
        val newClock = game.clock.map(c => if (rewindedSituation.color != color) c.takeback else c) map { clk =>
          game.clockHistory.flatMap(_.last(!takeBacker)).fold(clk) {
            t =>
              val nc = clk.setRemainingTime(!takeBacker, t)
              if (rewindedSituation.ghosts != 0) nc
              else if (chokeCapture)
                nc.takeTime(takeBacker, nc.timer.fold(draughts.Centis(0)) { tc => nc.timestamper.now - tc }).restart
              else if (rewindedSituation.color != takeBacker && rewindedGame.turns != initialPly)
                nc.takeTime(takeBacker, nc.timerFor(!takeBacker).fold(draughts.Centis(0)) { tc => nc.timestamper.now - tc }).restart
              else nc.restart
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
          clockHistory = game.clockHistory.map(_.update(rewindedSituation.color, _.dropRight(1))),
          movedAt = DateTime.now
        )
        Progress(game, newGame)
      }
  }
}
