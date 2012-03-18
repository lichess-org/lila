package lila.system

import model._
import memo._
import lila.chess.{ Color, White, Black }
import scalaz.effects._

final class InternalApi(
    repo: GameRepo,
    versionMemo: VersionMemo,
    aliveMemo: AliveMemo) {

  def join(fullId: String, url: String, messages: String): IO[Unit] = for {
    gameAndPlayer ← repo player fullId
    (g1, player) = gameAndPlayer
    g2 = g1 withEvents decodeMessages(messages)
    g3 = g2.withEvents(g2.opponent(player).color, List(RedirectEvent(url)))
    _ ← save(g1, g3)
  } yield ()

  def talk(gameId: String, author: String, message: String): IO[Unit] = for {
    g1 ← repo game gameId
    g2 = g1 withEvents List(MessageEvent(author, message))
    _ ← save(g1, g2)
  } yield ()

  def end(gameId: String, messages: String): IO[Unit] = for {
    g1 ← repo game gameId
    g2 = g1 withEvents (EndEvent() :: decodeMessages(messages))
    _ ← save(g1, g2)
  } yield ()

  def acceptRematch(
    gameId: String,
    newGameId: String,
    colorName: String,
    whiteRedirect: String,
    blackRedirect: String): IO[Unit] = for {
    color ← ioColor(colorName)
    g1 ← repo game gameId
    g2 = g1.withEvents(
      List(RedirectEvent(whiteRedirect)),
      List(RedirectEvent(blackRedirect)))
    _ ← save(g1, g2)
    _ ← aliveMemo.put(newGameId, !color)
    _ ← aliveMemo.transfer(gameId, !color, newGameId, color)
  } yield ()

  def updateVersion(gameId: String): IO[Unit] =
    repo game gameId flatMap versionMemo.put

  def reloadTable(gameId: String): IO[Unit] = for {
    g1 ← repo game gameId
    g2 = g1 withEvents List(ReloadTableEvent())
    _ ← save(g1, g2)
  } yield ()

  def alive(gameId: String, colorName: String): IO[Unit] = for {
    color ← ioColor(colorName)
    _ ← aliveMemo.put(gameId, color)
  } yield ()

  private def ioColor(colorName: String): IO[Color] = io {
    Color(colorName) err "Invalid color"
  }

  private def save(g1: DbGame, g2: DbGame): IO[Unit] = for {
    _ ← repo.applyDiff(g1, g2)
    _ ← versionMemo put g2
  } yield ()

  private def decodeMessages(messages: String): List[MessageEvent] =
    (messages split '$').toList map { MessageEvent("system", _) }
}
