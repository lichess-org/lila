package lila.game

import org.joda.time.DateTime
import cats.data.Validated

import shogi.format.{ Reader, Tag, Tags }
import shogi.format.forsyth.Sfen

object Rewind {

  private def createTags(sfen: Option[Sfen], game: Game) = {
    val variantTag = Some(Tag(_.Variant, game.variant.name))
    val sfenTag    = sfen.map(f => Tag(_.Sfen, f.value))

    Tags(List(variantTag, sfenTag).flatten)
  }

  def apply(game: Game, initialSfen: Option[Sfen]): Validated[String, Progress] =
    Reader
      .fromUsi(
        usis = game.usiMoves.dropRight(1),
        tags = createTags(initialSfen, game)
      )
      .valid
      .map { replay =>
        val rewindedGame = replay.state
        val color        = game.turnColor
        val turn         = rewindedGame.fullTurnNumber
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
            val moveTimes = BinaryFormat.moveTime.read(binary, game.playedPlies)
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
