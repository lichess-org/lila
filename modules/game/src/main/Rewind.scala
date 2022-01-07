package lila.game

import org.joda.time.DateTime
import cats.data.Validated

import shogi.format.{ FEN, Reader, Tag, Tags }

object Rewind {

  private def createTags(fen: Option[FEN], game: Game) = {
    val variantTag = Some(Tag(_.Variant, game.variant.name))
    val fenTag     = fen map (f => Tag(_.FEN, f.value))

    Tags(List(variantTag, fenTag).flatten)
  }

  def apply(game: Game, initialFen: Option[FEN]): Validated[String, Progress] =
    Reader
      .fromUsi(
        usis = game.usiMoves.dropRight(1),
        tags = createTags(initialFen, game)
      )
      .valid
      .map { replay =>
        val rewindedGame = replay.state
        val color        = game.turnColor
        val turn         = rewindedGame.fullMoveNumber
        val refundPeriod = ~(game.clockHistory map (_.countSpentPeriods(!color, turn)))

        val newClock = game.clock.map(_.refundPeriods(!color, refundPeriod).takeback) map { clk =>
          game.clockHistory
            .flatMap { ch =>
              if (ch.firstEnteredPeriod(color).exists(_ < turn)) clk.byoyomiOf(color).some
              else ch.last(color)
            }
            .fold(clk) { t =>
              clk.setRemainingTime(color, t)
            }
        }
        def rewindPlayer(player: Player) = player.copy(proposeTakebackAt = 0)
        val newGame = game.copy(
          sentePlayer = rewindPlayer(game.sentePlayer),
          gotePlayer = rewindPlayer(game.gotePlayer),
          shogi = rewindedGame.copy(clock = newClock),
          binaryMoveTimes = game.binaryMoveTimes.map { binary =>
            val moveTimes = BinaryFormat.moveTime.read(binary, game.playedTurns)
            BinaryFormat.moveTime.write(moveTimes.dropRight(1))
          },
          loadClockHistory = _ =>
            game.clockHistory.map { ch =>
              (ch.update(!color, _.dropRight(1))).refundSpentPeriods(!color, turn)
            },
          movedAt = DateTime.now
        )
        Progress(game, newGame)
      }
}
