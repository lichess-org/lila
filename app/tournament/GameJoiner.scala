package lila
package tournament

import game.{ DbGame, DbPlayer, GameRepo, Pov }
import user.User
import round.Meddler

import scalaz.effects._
import play.api.libs.concurrent._
import play.api.Play.current
import akka.util.duration._

final class GameJoiner(
    gameRepo: GameRepo,
    roundMeddler: Meddler,
    timelinePush: DbGame ⇒ IO[Unit],
    getUser: String ⇒ IO[Option[User]]) {

  def apply(tour: Started)(pairing: Pairing): IO[DbGame] = for {
    user1 ← getUser(pairing.user1) map (_ err "No such user " + pairing)
    user2 ← getUser(pairing.user2) map (_ err "No such user " + pairing)
    variant = chess.Variant.Standard
    game = DbGame(
      game = chess.Game(
        board = chess.Board init variant,
        clock = tour.clock.chessClock.some
      ),
      ai = None,
      whitePlayer = DbPlayer.white withUser user1,
      blackPlayer = DbPlayer.black withUser user2,
      creatorColor = chess.Color.White,
      mode = chess.Mode.Casual,
      variant = variant
    ).withTournamentId(tour.id)
      .withId(pairing.gameId)
      .start
      .startClock(2)
    _ ← gameRepo insert game
    _ ← gameRepo denormalizeStarted game
    _ ← timelinePush(game)
    _ ← scheduleIdleCheck(game.id)
  } yield game

  private def scheduleIdleCheck(gameId: String) = io {
    Akka.system.scheduler.scheduleOnce(20 seconds)(idleCheck(gameId))
  }

  private def idleCheck(gameId: String) {
    (for {
      gameOption ← gameRepo game gameId
      _ ← gameOption.fold(
        game ⇒ game.playerWhoDidNotMove.fold(
          player ⇒ roundMeddler resign Pov(game, player),
          io()
        ),
        io()
      )
    } yield ()).unsafePerformIO
  }
}
