package lila.system

import model._
import memo._
import db._
import scalaz.effects._

case class LobbyApi(
    hookRepo: HookRepo,
    gameRepo: GameRepo,
    entryRepo: EntryRepo,
    lobbyMemo: LobbyMemo,
    entryMemo: EntryMemo,
    versionMemo: VersionMemo,
    aliveMemo: AliveMemo,
    hookMemo: HookMemo) extends IOTools {

  def join(
    gameId: String,
    colorName: String,
    entryGame: EntryGame): IO[Unit] = for {
    color ← ioColor(colorName)
    g1 ← gameRepo game gameId
    _ ← aliveMemo.put(gameId, color)
    _ ← aliveMemo.put(gameId, !color)
    _ ← versionInc
    _ ← addEntry(entryGame)
  } yield ()

  def create(hookOwnerId: String): IO[Unit] = for {
    _ ← versionInc
    _ ← hookMemo put hookOwnerId
  } yield ()

  def remove(hookId: String): IO[Unit] = for {
    _ ← hookRepo removeId hookId
    _ ← versionInc
  } yield ()

  def alive(hookOwnerId: String): IO[Unit] = hookMemo put hookOwnerId

  private[system] def versionInc: IO[Int] = lobbyMemo++

  private[system] def addEntry(entryGame: EntryGame): IO[Unit] = for {
    nextId ← (entryMemo++)
    _ ← io { entryRepo insert Entry(nextId, entryGame) }
  } yield ()
}
