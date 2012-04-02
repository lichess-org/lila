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

  def show(fullId: String): IO[Map[String, Any]] = for {
    pov ← gameRepo pov fullId
    _ ← aliveMemo.put(pov.game.id, pov.color)
    roomHtml ← messenger render pov.game.id
  } yield Map(
    "stackVersion" -> pov.player.eventStack.lastVersion,
    "roomHtml" -> roomHtml,
    "opponentActivity" -> aliveMemo.activity(pov.game.id, !pov.color),
    "possibleMoves" -> {
      if (pov.game playableBy pov.player)
        pov.game.toChess.situation.destinations map {
          case (from, dests) ⇒ from.key -> (dests.mkString)
        } toMap
      else null
    }
  )

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

  def start(gameId: String, entryData: String): IO[Unit] = for {
    g1 ← gameRepo game gameId
    g2 ← starter.start(g1, entryData)
    _ ← save(g1, g2)
  } yield ()

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
    // tell spectators to reload the table
    g3 = g2.withEvents(List(ReloadTableEvent()))
    _ ← save(g1, g3)
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

  def playerVersion(gameId: String, colorName: String): IO[Int] = for {
    color ← ioColor(colorName)
    pov ← gameRepo.pov(gameId, color)
  } yield pov.player.eventStack.lastVersion

  def activity(gameId: String, colorName: String): Int =
    Color(colorName).fold(aliveMemo.activity(gameId, _), 0)
}
