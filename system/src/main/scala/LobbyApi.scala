package lila.system

import model._
import memo._
import db._
import scalaz.effects._

final class LobbyApi(
    hookRepo: HookRepo,
    val gameRepo: GameRepo,
    messenger: Messenger,
    starter: Starter,
    lobbyMemo: LobbyMemo,
    messageMemo: MessageMemo,
    val versionMemo: VersionMemo,
    aliveMemo: AliveMemo,
    hookMemo: HookMemo) extends IOTools {

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

  def messageRefresh: IO[Unit] = messageMemo.refresh

  private[system] def versionInc: IO[Int] = lobbyMemo++
}
