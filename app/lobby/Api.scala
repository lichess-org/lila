package lila
package lobby

import model._
import memo._
import db._
import scalaz.effects._

final class Api(
    hookRepo: HookRepo,
    val gameRepo: GameRepo,
    messenger: Messenger,
    starter: Starter,
    lobbyMemo: LobbyMemo,
    val versionMemo: VersionMemo,
    aliveMemo: AliveMemo,
    hookMemo: HookMemo) extends IOTools {

  def cancel(ownerId: String): IO[Unit] = for {
    _ ← hookRepo removeOwnerId ownerId
    _ ← hookMemo remove ownerId
    _ ← versionInc
  } yield ()

  def join(
    gameId: String,
    colorName: String,
    entryData: String,
    messageString: String): IO[Unit] = for {
    color ← ioColor(colorName)
    game ← gameRepo game gameId
    g2 ← messenger.systemMessages(game, messageString)
    g3 ← starter.start(g2, entryData)
    _ ← save(game, g3)
    _ ← aliveMemo.put(gameId, color)
    _ ← aliveMemo.put(gameId, !color)
    _ ← versionInc
  } yield ()

  def create(hookOwnerId: String): IO[Unit] = for {
    _ ← hookMemo put hookOwnerId
    _ ← versionInc
  } yield ()

  def alive(hookOwnerId: String): IO[Unit] = hookMemo put hookOwnerId

  def versionInc: IO[Int] = lobbyMemo++
}
