package lila
package lobby

import model._
import memo._
import db._
import lila.chess.Color
import scalaz.effects._

final class Api(
    hookRepo: HookRepo,
    fisherman: Fisherman,
    gameRepo: GameRepo,
    gameSocket: game.Socket,
    messenger: Messenger,
    starter: Starter,
    lobbySocket: lobby.Socket) {

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
    myHookOwnerId: Option[String]): IO[Unit] = for {
    hook ← hookRepo ownedHook hookOwnerId
    color ← ioColor(colorName)
    game ← gameRepo game gameId
    p1 ← starter.start(game, entryData)
    p2 ← messenger.systemMessages(game, messageString) map p1.++
    _ ← gameRepo save p2
    _ ← gameSocket send p2
    _ ← hook.fold(h ⇒ fisherman.bite(h, p2.game), io())
    _ ← myHookOwnerId.fold(
      ownerId ⇒ hookRepo ownedHook ownerId flatMap { myHook ⇒
        myHook.fold(fisherman.delete, io())
      },
      io())
  } yield ()

  def create(hookOwnerId: String): IO[Unit] = for {
    hook ← hookRepo ownedHook hookOwnerId
    _ ← hook.fold(fisherman.+, io())
  } yield ()

  private def ioColor(colorName: String): IO[Color] = io {
    Color(colorName) err "Invalid color"
  }
}
