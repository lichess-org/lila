package lila
package lobby

import model._
import memo._
import db._
import scalaz.effects._

final class Api(
    hookRepo: HookRepo,
    fisherman: Fisherman,
    val gameRepo: GameRepo,
    messenger: Messenger,
    starter: Starter,
    lobbySocket: lobby.Lobby,
    val versionMemo: VersionMemo,
    aliveMemo: AliveMemo) extends IOTools {

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
    g2 ← messenger.systemMessages(game, messageString)
    g3 ← starter.start(g2, entryData)
    _ ← save(game, g3)
    _ ← aliveMemo.put(gameId, color)
    _ ← aliveMemo.put(gameId, !color)
    _ ← hook.fold(h ⇒ fisherman.bite(h, g3), io())
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
}
