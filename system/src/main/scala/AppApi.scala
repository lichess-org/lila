package lila.system

import model._
import memo._
import db.{ GameRepo, RoomRepo }
import lila.chess.{ Color, White, Black }
import scalaz.effects._

final class AppApi(
    val gameRepo: GameRepo,
    val versionMemo: VersionMemo,
    aliveMemo: AliveMemo,
    messenger: Messenger,
    starter: Starter) extends IOTools {

  def join(
    fullId: String,
    url: String,
    messages: String,
    entryData: String): IO[Unit] = for {
    pov ← gameRepo pov fullId
    g2 ← starter.start(pov.game, entryData)
    g3 ← messenger.systemMessages(g2, messages)
    g4 = g3.withEvents(!pov.color, List(RedirectEvent(url)))
    _ ← save(pov.game, g4)
  } yield ()

  def start(gameId: String, entryData: String): IO[Unit] =
    starter.start(gameId, entryData) map (_ ⇒ Unit)

  def rematchAccept(
    gameId: String,
    newGameId: String,
    colorName: String,
    whiteRedirect: String,
    blackRedirect: String,
    entryData: String,
    messageString: String): IO[Unit] = for {
    color ← ioColor(colorName)
    newGame ← gameRepo game newGameId
    g1 ← gameRepo game gameId
    g2 = g1.withEvents(
      List(RedirectEvent(whiteRedirect)),
      List(RedirectEvent(blackRedirect)))
    _ ← save(g1, g2)
    ng2 ← messenger.systemMessages(newGame, messageString)
    ng3 ← starter.start(ng2, entryData)
    _ ← save(newGame, ng3)
    _ ← aliveMemo.put(newGameId, !color)
    _ ← aliveMemo.transfer(gameId, !color, newGameId, color)
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
    g2 ← messenger.systemMessages(g1, messages)
    g3 = g2.withEvents(!color, List(ReloadTableEvent()))
    _ ← save(g1, g3)
  } yield ()

  def activity(gameId: String, colorName: String): Int =
    Color(colorName) some { aliveMemo.activity(gameId, _) } none 0

  def room(gameId: String): IO[String] =
    messenger render gameId

  def possibleMoves(gameId: String, colorName: String): IO[Map[String, Any]] =
    for {
      color ← ioColor(colorName)
      game ← gameRepo game gameId
    } yield game.toChess.situation.destinations map {
      case (from, dests) ⇒ from.key -> (dests.mkString)
    } toMap
}
