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
    aliveMemo: AliveMemo) extends IOTools {

  def join(gameId: String, colorName: String): IO[Unit] = for {
    color ← ioColor(colorName)
    g1 ← gameRepo game gameId
    _ ← aliveMemo.put(gameId, color)
    _ ← aliveMemo.put(gameId, !color)
  } yield ()
}
