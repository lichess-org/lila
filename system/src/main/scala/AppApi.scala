package lila.system

import model._
import memo._
import db.GameRepo
import lila.chess.{ Color, White, Black }
import scalaz.effects._

case class AppApi(
    gameRepo: GameRepo,
    ai: Ai,
    versionMemo: VersionMemo,
    aliveMemo: AliveMemo,
    addEntry: (DbGame, String) ⇒ IO[Unit]) extends IOTools {

  def join(
    fullId: String,
    url: String,
    messages: String,
    entryData: String): IO[Unit] = for {
    gameAndPlayer ← gameRepo player fullId
    (g1, player) = gameAndPlayer
    g2 = g1 withEvents decodeMessages(messages)
    g3 = g2.withEvents(g2.opponent(player).color, List(RedirectEvent(url)))
    _ ← save(g1, g3)
    _ ← addEntry(g3, entryData)
  } yield ()

  def talk(gameId: String, author: String, message: String): IO[Unit] = for {
    g1 ← gameRepo game gameId
    g2 = g1 withEvents List(MessageEvent(author, message))
    _ ← save(g1, g2)
  } yield ()

  def end(gameId: String, messages: String): IO[Unit] = for {
    g1 ← gameRepo game gameId
    g2 = g1 withEvents (EndEvent() :: decodeMessages(messages))
    _ ← save(g1, g2)
  } yield ()

  def start(gameId: String, entryData: String): IO[Unit] = for {
    game ← gameRepo game gameId
    _ ← addEntry(game, entryData)
    _ ← if (game.player.isAi) for {
      aiResult ← ai(game) map (_.toOption err "AI failure")
      (newChessGame, move) = aiResult
      _ ← save(game, game.update(newChessGame, move))
    } yield ()
    else io()
  } yield ()

  def rematchAccept(
    gameId: String,
    newGameId: String,
    colorName: String,
    whiteRedirect: String,
    blackRedirect: String,
    entryData: String): IO[Unit] = for {
    color ← ioColor(colorName)
    g1 ← gameRepo game gameId
    g2 = g1.withEvents(
      List(RedirectEvent(whiteRedirect)),
      List(RedirectEvent(blackRedirect)))
    _ ← save(g1, g2)
    _ ← aliveMemo.put(newGameId, !color)
    _ ← aliveMemo.transfer(gameId, !color, newGameId, color)
    _ ← addEntry(g2, entryData)
  } yield ()

  def updateVersion(gameId: String): IO[Unit] =
    gameRepo game gameId flatMap versionMemo.put

  def reloadTable(gameId: String): IO[Unit] = for {
    g1 ← gameRepo game gameId
    g2 = g1 withEvents List(ReloadTableEvent())
    _ ← save(g1, g2)
  } yield ()

  def alive(gameId: String, colorName: String): IO[Unit] = for {
    color ← ioColor(colorName)
    _ ← aliveMemo.put(gameId, color)
  } yield ()

  def draw(gameId: String, colorName: String, messages: String): IO[Unit] = for {
    color ← ioColor(colorName)
    g1 ← gameRepo game gameId
    g2 = g1 withEvents decodeMessages(messages)
    g3 = g2.withEvents(!color, List(ReloadTableEvent()))
    _ ← save(g1, g3)
  } yield ()

  def drawAccept(gameId: String, colorName: String, messages: String): IO[Unit] = for {
    color ← ioColor(colorName)
    g1 ← gameRepo game gameId
    g2 = g1 withEvents (EndEvent() :: decodeMessages(messages))
    _ ← save(g1, g2)
  } yield ()

  def activity(gameId: String, colorName: String): Int =
    Color(colorName) some { aliveMemo.activity(gameId, _) } none 0

  def possibleMoves(gameId: String, colorName: String): IO[Map[String, Any]] =
    for {
      color ← ioColor(colorName)
      game ← gameRepo game gameId
    } yield game.toChess.situation.destinations map {
      case (from, dests) ⇒ from.key -> (dests.mkString)
    } toMap

  private def decodeMessages(messages: String): List[MessageEvent] =
    (messages split '$').toList map { MessageEvent("system", _) }
}
