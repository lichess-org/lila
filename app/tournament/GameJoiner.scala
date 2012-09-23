package lila
package tournament

import chess.Color
import game.{ DbGame, DbPlayer, GameRepo, Pov, PovRef }
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

  private val secondsToMove = 20

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
    _ ← scheduleIdleCheck(PovRef(game.id, Color.White), secondsToMove)
  } yield game

  private def scheduleIdleCheck(povRef: PovRef, in: Int) = io {
    Akka.system.scheduler.scheduleOnce(in seconds)(idleCheck(povRef))
  } map (_ ⇒ ())

  private def idleCheck(povRef: PovRef) {
    (for {
      povOption ← gameRepo pov povRef
      _ ← povOption.filter(_.game.playable).fold(idleResult, io())
    } yield ()).unsafePerformIO
  }

  private def idleResult(pov: Pov) = {
    val idle = !pov.game.playerHasMoved(pov.color)
    idle.fold(
      roundMeddler resign pov,
      (pov.color.white && !pov.game.playerHasMoved(Color.Black)).fold(
        scheduleIdleCheck(!pov.ref, pov.game.lastMoveTime.fold(
          lastMoveTime ⇒ lastMoveTime - nowSeconds + secondsToMove,
          secondsToMove
        )),
        io()
      )
    )
  }
}
