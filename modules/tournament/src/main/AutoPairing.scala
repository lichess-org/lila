package lila.tournament

import scala.concurrent.duration._

import akka.actor.{ ActorRef, ActorSystem, ActorSelection }

import chess.Color
import lila.game.{ Game, Player => GamePlayer, GameRepo, Pov, PovRef, Source, PerfPicker }
import lila.hub.actorApi.map.Tell
import lila.round.actorApi.round.NoStartColor
import lila.user.{ User, UserRepo }

object SecondsToDoFirstMove {
  def secondsToMoveFor(tour: Tournament) = tour.speed match {
    case chess.Speed.Bullet => 20
    case chess.Speed.Blitz  => 25
    case _                  => 30
  }
}

final class AutoPairing(
    roundMap: ActorRef,
    system: ActorSystem,
    onStart: String => Unit) {

  def apply(tour: Tournament, pairing: Pairing): Fu[Game] = for {
    user1 ← getUser(pairing.user1)
    user2 ← getUser(pairing.user2)
    game1 = Game.make(
      game = chess.Game(
        variant = tour.variant.some,
        fen = tour.position.some.filterNot(_.initial).map(_.fen)
      ) |> { g =>
          val turns = g.player.fold(0, 1)
          g.copy(
            clock = tour.clock.chessClock.some,
            turns = turns,
            startedAtTurn = turns)
        },
      whitePlayer = GamePlayer.white,
      blackPlayer = GamePlayer.black,
      mode = tour.mode,
      variant =
        if (tour.position.initial) tour.variant
        else chess.variant.FromPosition,
      source = Source.Tournament,
      pgnImport = None)
    game2 = game1
      .updatePlayer(Color.White, _.withUser(user1.id, PerfPicker.mainOrDefault(game1)(user1.perfs)))
      .updatePlayer(Color.Black, _.withUser(user2.id, PerfPicker.mainOrDefault(game1)(user2.perfs)))
      .withTournamentId(tour.id)
      .withId(pairing.gameId)
      .start
    _ ← (GameRepo insertDenormalized game2) >>-
      scheduleIdleCheck(PovRef(game2.id, game2.turnColor), SecondsToDoFirstMove.secondsToMoveFor(tour), true) >>-
      onStart(game2.id)
  } yield game2

  private def getUser(username: String): Fu[User] =
    UserRepo named username flatMap {
      _.fold(fufail[User]("No user named " + username))(fuccess)
    }

  private def scheduleIdleCheck(povRef: PovRef, secondsToMove: Int, thenAgain: Boolean) {
    system.scheduler.scheduleOnce(secondsToMove seconds)(idleCheck(povRef, secondsToMove, thenAgain))
  }

  private def idleCheck(povRef: PovRef, secondsToMove: Int, thenAgain: Boolean) {
    GameRepo pov povRef foreach {
      _.filter(_.game.playable) foreach { pov =>
        if (pov.game.playerHasMoved(pov.color)) {
          if (thenAgain && !pov.game.playerHasMoved(pov.opponent.color))
            scheduleIdleCheck(!pov.ref, pov.game.lastMoveTimeInSeconds.fold(secondsToMove) { lmt =>
              lmt - nowSeconds + secondsToMove
            }, false)
        }
        else roundMap ! Tell(pov.gameId, NoStartColor(pov.color))
      }
    }
  }
}
