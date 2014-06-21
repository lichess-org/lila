package lila.pool

import scala.concurrent.duration._

import akka.actor.{ ActorRef, ActorSystem, ActorSelection }

import chess.Color
import lila.game.{ Game, Player => GamePlayer, GameRepo, Pov, PovRef, Source }
import lila.hub.actorApi.map.Tell
import lila.round.actorApi.round.NoStartColor
import lila.user.{ User, UserRepo }

final class Joiner(
    roundMap: ActorRef,
    system: ActorSystem,
    secondsToMove: Int) {

  def apply(setup: PoolSetup, pairings: List[Pairing]): Fu[List[PairingWithGame]] =
    pairings.map { pairing =>
      startGame(setup, pairing) map {
        PairingWithGame(pairing, _)
      }
    }.sequenceFu

  def startGame(setup: PoolSetup, pairing: Pairing): Fu[Game] = for {
    user1 ← getUser(pairing.user1)
    user2 ← getUser(pairing.user2)
    ratingLens = (user: User) => user.perfs.pool(setup.id).intRating
    game = Game.make(
      game = chess.Game(
        board = chess.Board init setup.variant,
        clock = setup.clock.some
      ),
      whitePlayer = GamePlayer.white withUser user1 withRating ratingLens(user1),
      blackPlayer = GamePlayer.black withUser user2 withRating ratingLens(user2),
      mode = chess.Mode.Rated,
      variant = setup.variant,
      source = Source.Pool,
      pgnImport = None
    ).withPoolId(setup.id)
      .withId(pairing.gameId)
      .start
    _ ← (GameRepo insertDenormalized game) >>-
      scheduleIdleCheck(PovRef(game.id, Color.White), secondsToMove)
  } yield game

  private def getUser(userId: String): Fu[User] =
    UserRepo byId userId flatten s"No user $userId"

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
          roundMap ! Tell(pov.gameId, NoStartColor(pov.color))
        )
      }
    }
  }
}
