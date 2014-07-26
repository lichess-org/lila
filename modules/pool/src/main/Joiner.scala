package lila.pool

import scala.concurrent.duration._

import akka.actor.{ ActorRef, ActorSystem, ActorSelection }

import chess.Color
import lila.game.{ Game, Player => GamePlayer, GameRepo, Pov, PovRef, Source, PerfPicker }
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

  private def startGame(setup: PoolSetup, pairing: Pairing): Fu[Game] = for {
    user1 ← getUser(pairing.user1)
    user2 ← getUser(pairing.user2)
    game1 = Game.make(
      game = chess.Game(
        board = chess.Board init setup.variant,
        clock = setup.clock.some
      ),
      whitePlayer = GamePlayer.white,
      blackPlayer = GamePlayer.black,
      mode = chess.Mode.Rated,
      variant = setup.variant,
      source = Source.Pool,
      pgnImport = None
    ).withPoolId(setup.id).withId(pairing.gameId)
    game2 = game1
      .updatePlayer(Color.White, _.withUser(user1.id, PerfPicker.mainOrDefault(game1)(user1.perfs)))
      .updatePlayer(Color.Black, _.withUser(user2.id, PerfPicker.mainOrDefault(game1)(user2.perfs)))
      .start
    _ ← (GameRepo insertDenormalized game2) >>-
      scheduleIdleCheck(PovRef(game2.id, Color.White), secondsToMove)
  } yield game2

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
