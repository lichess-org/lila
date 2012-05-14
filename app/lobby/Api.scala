package lila
package lobby

import game.{ GameRepo, Socket ⇒ GameSocket, Messenger ⇒ GameMessenger }
import chess.Color

import scalaz.effects._

final class Api(
    hookRepo: HookRepo,
    fisherman: Fisherman,
    gameRepo: GameRepo,
    gameSocket: GameSocket,
    gameMessenger: GameMessenger,
    starter: Starter) {

  def cancel(ownerId: String): IO[Unit] = for {
    hook ← hookRepo ownedHook ownerId
    _ ← hook.fold(fisherman.delete, io())
  } yield ()

  def join(
    gameId: String,
    colorName: String,
    entryData: String,
    messageString: String,
    hookOwnerId: String,
    myHookOwnerId: Option[String]): IO[Valid[Unit]] = for {
    hook ← hookRepo ownedHook hookOwnerId
    gameOption ← gameRepo game gameId
    result ← (Color(colorName) |@| gameOption).apply(
      (color, game) ⇒ {
        for {
          p1 ← starter.start(game, entryData)
          p2 ← gameMessenger.systemMessages(game, messageString) map p1.++
          _ ← gameRepo save p2
          _ ← gameSocket send p2
          _ ← hook.fold(h ⇒ fisherman.bite(h, p2.game), io())
          _ ← myHookOwnerId.fold(
            ownerId ⇒ hookRepo ownedHook ownerId flatMap { myHook ⇒
              myHook.fold(fisherman.delete, io())
            },
            io())
        } yield success()
      }
    ).fold(identity, io(GameNotFound))
  } yield result

  def create(hookOwnerId: String): IO[Unit] = for {
    hook ← hookRepo ownedHook hookOwnerId
    _ ← hook.fold(fisherman.+, io())
  } yield ()
}
