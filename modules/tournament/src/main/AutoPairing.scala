package lila.tournament

import scala.concurrent.duration._

import akka.actor.{ ActorRef, ActorSystem }

import chess.Color
import lila.game.{ Game, Player => GamePlayer, GameRepo, PovRef, Source, PerfPicker }
import lila.hub.actorApi.map.Tell
import lila.round.actorApi.round.NoStartColor
import lila.user.User

object SecondsToDoFirstMove {
  def secondsToMoveFor(tour: Tournament) = tour.speed match {
    case chess.Speed.UltraBullet => 15
    case chess.Speed.Bullet => 20
    case chess.Speed.Blitz => 25
    case _ => 30
  }
}

final class AutoPairing(
    roundMap: ActorRef,
    system: ActorSystem,
    onStart: String => Unit
) {

  def apply(tour: Tournament, pairing: Pairing, usersMap: Map[User.ID, User]): Fu[Game] = {
    val user1 = usersMap get pairing.user1 err s"Missing pairing user $pairing"
    val user2 = usersMap get pairing.user2 err s"Missing pairing user $pairing"
    val game1 = Game.make(
      game = chess.Game(
        variantOption = tour.variant.some,
        fen = tour.position.some.filterNot(_.initial).map(_.fen)
      ) |> { g =>
          val turns = g.player.fold(0, 1)
          g.copy(
            clock = tour.clock.toClock.some,
            turns = turns,
            startedAtTurn = turns
          )
        },
      whitePlayer = GamePlayer.white,
      blackPlayer = GamePlayer.black,
      mode = tour.mode,
      variant =
        if (tour.position.initial) tour.variant
        else chess.variant.FromPosition,
      source = Source.Tournament,
      pgnImport = None
    )
    val game2 = game1
      .updatePlayer(Color.White, _.withUser(user1.id, PerfPicker.mainOrDefault(game1)(user1.perfs)))
      .updatePlayer(Color.Black, _.withUser(user2.id, PerfPicker.mainOrDefault(game1)(user2.perfs)))
      .withTournamentId(tour.id)
      .withId(pairing.gameId)
      .start
    (GameRepo insertDenormalized game2) >>-
      scheduleIdleCheck(PovRef(game2.id, game2.turnColor), SecondsToDoFirstMove.secondsToMoveFor(tour), true) >>-
      onStart(game2.id) inject
      game2
  }

  private def scheduleIdleCheck(povRef: PovRef, secondsToMove: Int, thenAgain: Boolean) {
    system.scheduler.scheduleOnce(secondsToMove seconds)(idleCheck(povRef, secondsToMove, thenAgain))
  }

  private def idleCheck(povRef: PovRef, secondsToMove: Int, thenAgain: Boolean) {
    GameRepo pov povRef foreach {
      _.filter(_.game.playable) foreach { pov =>
        if (pov.game.playerHasMoved(pov.color)) {
          if (thenAgain && !pov.game.playerHasMoved(pov.opponent.color))
            scheduleIdleCheck(
              !pov.ref,
              (pov.game.movedAt.getSeconds - nowSeconds + secondsToMove).toInt,
              false
            )
        } else roundMap ! Tell(pov.gameId, NoStartColor(pov.color))
      }
    }
  }
}
