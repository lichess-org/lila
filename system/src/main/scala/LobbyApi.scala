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
    messageMemo: MessageMemo,
    entryMemo: EntryMemo,
    versionMemo: VersionMemo,
    aliveMemo: AliveMemo,
    hookMemo: HookMemo) extends IOTools {

  def join(
    gameId: String,
    colorName: String,
    entryData: String): IO[Unit] = for {
    color ← ioColor(colorName)
    game ← gameRepo game gameId
    _ ← aliveMemo.put(gameId, color)
    _ ← aliveMemo.put(gameId, !color)
    _ ← versionInc
    _ ← addEntry(game, entryData)
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

  def messageRefresh: IO[Unit] = messageMemo.refresh

  private[system] def versionInc: IO[Int] = lobbyMemo++

  private[system] def addEntry(game: DbGame, data: String): IO[Unit] =
    Entry.build(game, data).fold(
      f ⇒ (entryMemo++) map (id ⇒ entryRepo insert f(id)),
      io()
    )
}
