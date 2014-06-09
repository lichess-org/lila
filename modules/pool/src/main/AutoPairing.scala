package lila.pool

import scala.concurrent.duration._

import akka.actor.{ ActorRef, ActorSystem, ActorSelection }

import chess.Color
import lila.game.{ Game, Player => GamePlayer, GameRepo, Pov, PovRef, Source }
import lila.hub.actorApi.map.Tell
import lila.round.actorApi.round.ResignColor
import lila.user.{ User, UserRepo }

final class AutoPairing(
    roundMap: ActorRef,
    system: ActorSystem) {

  private val secondsToMove = 30

  def apply(pool: Pool, userIds: Set[String]): Fu[List[PairingWithGame]] = {
    val playingUserIds = pool.pairings.filter(_.playing).flatMap(_.users).toSet
    def inPoolRoom(p: Player) = userIds contains p.user.id
    def isPlaying(p: Player) = playingUserIds contains p.user.id
    val availablePlayers = pool.players filter inPoolRoom filterNot isPlaying
    (availablePlayers.sortBy(-_.rating) grouped 2).map {
      case List(p1, p2) =>
        val pairing = Pairing(p1.user.id, p2.user.id)
        startGame(pool, pairing) map {
          PairingWithGame(pairing, _).some
        }
      case _ => fuccess(none)
    }.sequenceFu map (_.flatten.toList)
  }

  def startGame(pool: Pool, pairing: Pairing): Fu[Game] = for {
    user1 ← getUser(pairing.user1)
    user2 ← getUser(pairing.user2)
    game = Game.make(
      game = chess.Game(
        board = chess.Board init pool.setup.variant,
        clock = pool.setup.clock.some
      ),
      whitePlayer = GamePlayer.white withUser user1,
      blackPlayer = GamePlayer.black withUser user2,
      mode = pool.setup.mode,
      variant = pool.setup.variant,
      source = Source.Pool,
      pgnImport = None
    ).withPoolId(pool.setup.id)
      .withId(pairing.gameId)
      .start
      .startClock(2)
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
          roundMap ! Tell(pov.gameId, ResignColor(pov.color))
        )
      }
    }
  }
}
