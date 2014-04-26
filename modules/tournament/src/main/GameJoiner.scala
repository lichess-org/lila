package lila.tournament

import scala.concurrent.duration._

import akka.actor.{ ActorRef, ActorSystem, ActorSelection }

import chess.Color
import lila.game.{ Game, Player => GamePlayer, GameRepo, Pov, PovRef, Source }
import lila.hub.actorApi.map.Tell
import lila.round.actorApi.round.ResignColor
import lila.user.{ User, UserRepo }

final class GameJoiner(
    roundMap: ActorRef,
    system: ActorSystem) {

  private val secondsToMove = 30

  def apply(tour: Started)(pairing: Pairing): Fu[Game] = for {
    user1 ← getUser(pairing.user1)
    user2 ← getUser(pairing.user2)
    game = Game.make(
      game = chess.Game(
        board = chess.Board init tour.variant,
        clock = tour.clock.chessClock.some
      ),
      whitePlayer = GamePlayer.white withUser user1,
      blackPlayer = GamePlayer.black withUser user2,
      mode = tour.mode,
      variant = tour.variant,
      source = Source.Tournament,
      pgnImport = None
    ).withTournamentId(tour.id)
      .withId(pairing.gameId)
      .start
      .startClock(2)
    _ ← (GameRepo insertDenormalized game) >>-
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
      _.filter(_.game.playable) foreach { pov =>
        pov.game.playerHasMoved(pov.color).fold(
          (pov.color.white && !pov.game.playerHasMoved(Color.Black)) ?? {
            scheduleIdleCheck(!pov.ref, pov.game.lastMoveTimeInSeconds.fold(secondsToMove) { lmt =>
              lmt - nowSeconds + secondsToMove
            })
          },
          roundMap ! Tell(pov.gameId, ResignColor(pov.color))
        )
      }
    }
  }
}
