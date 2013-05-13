package lila.tournament

import chess.Color
import lila.game.{ Game, Player ⇒ GamePlayer, GameRepo, Pov, PovRef, Source }
import lila.user.{ User, UserRepo }
import lila.round.Meddler

import scala.concurrent.duration._
import akka.actor.{ ActorRef, ActorSystem }

final class GameJoiner(
    roundMeddler: Meddler,
    timelinePush: lila.hub.ActorLazyRef,
    system: ActorSystem) {

  private val secondsToMove = 20

  def apply(tour: Started)(pairing: Pairing): Fu[Game] = for {
    user1 ← getUser(pairing.user1)
    user2 ← getUser(pairing.user2)
    game = Game.make(
      game = chess.Game(
        board = chess.Board init tour.variant,
        clock = tour.clock.chessClock.some
      ),
      ai = None,
      whitePlayer = GamePlayer.white withUser user1,
      blackPlayer = GamePlayer.black withUser user2,
      creatorColor = chess.Color.White,
      mode = tour.mode,
      variant = tour.variant,
      source = Source.Tournament,
      pgnImport = None
    ).withTournamentId(tour.id)
      .withId(pairing.gameId)
      .start
      .startClock(2)
    _ ← (GameRepo insertDenormalized game) >>-
      (timelinePush ! game) >>-
      scheduleIdleCheck(PovRef(game.id, Color.White), secondsToMove)
  } yield game

  private def getUser(username: String): Fu[User] =
    UserRepo named username flatMap {
      _.fold(fufail[User]("No user named " + username))(fuccess)
    }

  private def scheduleIdleCheck(povRef: PovRef, in: Int) {
    system.scheduler.scheduleOnce(in seconds)(idleCheck(povRef))
  }

  private def idleCheck(povRef: PovRef) {
    GameRepo pov povRef foreach {
      _.filter(_.game.playable) foreach { pov ⇒
        pov.game.playerHasMoved(pov.color).fold(
          (pov.color.white && !pov.game.playerHasMoved(Color.Black)) ?? {
            scheduleIdleCheck(!pov.ref, pov.game.lastMoveTime.fold(secondsToMove) { lmt ⇒
              lmt - nowSeconds + secondsToMove
            })
          },
          roundMeddler resign pov)
      }
    }
  }
}
