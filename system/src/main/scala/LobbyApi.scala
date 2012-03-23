package lila.system

import model._
import memo._
import db.{ GameRepo, HookRepo }
import scalaz.effects._

case class LobbyApi(
    hookRepo: HookRepo,
    lobbyMemo: LobbyMemo,
    versionMemo: VersionMemo,
    gameRepo: GameRepo,
    aliveMemo: AliveMemo,
    hookMemo: HookMemo) extends IOTools {

  def join(gameId: String, colorName: String): IO[Unit] = for {
    color ← ioColor(colorName)
    g1 ← gameRepo game gameId
    _ ← aliveMemo.put(gameId, color)
    _ ← aliveMemo.put(gameId, !color)
    _ ← (lobbyMemo++)
  } yield ()

  def inc: IO[Unit] = lobbyMemo++

  def create(hookOwnerId: String): IO[Unit] = for {
    _ ← (lobbyMemo++)
    _ ← hookMemo put hookOwnerId
  } yield ()

  def remove(hookId: String): IO[Unit] = for {
    _ ← hookRepo removeId hookId
    _ ← (lobbyMemo++)
  } yield ()
}
