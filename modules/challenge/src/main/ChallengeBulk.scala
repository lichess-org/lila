package lila.challenge

import akka.stream.scaladsl._
import chess.variant.Variant
import chess.{ Clock, Mode, Situation, Speed }
import org.joda.time.DateTime
import scala.util.chaining._

import lila.common.LilaStream
import lila.game.{ Game, Player }
import lila.rating.PerfType
import lila.setup.SetupBulk.ScheduledBulk
import lila.user.User

final class ChallengeBulkApi(
    gameRepo: lila.game.GameRepo,
    userRepo: lila.user.UserRepo,
    onStart: lila.round.OnStart
)(implicit
    ec: scala.concurrent.ExecutionContext,
    mat: akka.stream.Materializer
) {

  def apply(
      by: User,
      scheduled: ScheduledBulk
  ): Fu[Int] = {
    val perfType   = PerfType(scheduled.variant, Speed(scheduled.clock))
    val startClock = scheduled.startClocksAt isBefore DateTime.now
    Source(scheduled.games)
      .mapAsyncUnordered(8) { game =>
        userRepo.pair(game.white, game.black) map2 { case (white, black) =>
          (game.id, white, black)
        }
      }
      .mapConcat(_.toList)
      .map[Game] { case (id, white, black) =>
        Game
          .make(
            chess =
              chess.Game(situation = Situation(scheduled.variant), clock = scheduled.clock.toClock.some),
            whitePlayer = Player.make(chess.White, white.some, _(perfType)),
            blackPlayer = Player.make(chess.Black, black.some, _(perfType)),
            mode = scheduled.mode,
            source = lila.game.Source.Api,
            pgnImport = None
          )
          .withId(id)
          .start
          .pipe { g =>
            if (startClock) g.startClock.fold(g)(_.game) else g
          }
      }
      .mapAsyncUnordered(8) { game =>
        (gameRepo insertDenormalized game) >>- onStart(game.id)
      }
      .toMat(LilaStream.sinkCount)(Keep.right)
      .run()
      .addEffect { nb =>
        lila.mon.api.challenge.bulk.createNb(by.id).increment(nb).unit
      }
  }
}
