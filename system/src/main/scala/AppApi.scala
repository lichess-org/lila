package lila.system

import model._
import memo._
import db.{ GameRepo, RoomRepo }
import lila.chess.{ Color, White, Black }
import scalaz.effects._

case class AppApi(
    gameRepo: GameRepo,
    roomRepo: RoomRepo,
    ai: Ai,
    versionMemo: VersionMemo,
    aliveMemo: AliveMemo,
    addEntry: (DbGame, String) ⇒ IO[Unit]) extends IOTools {

  def join(
    fullId: String,
    url: String,
    messages: String,
    entryData: String): IO[Unit] = for {
    pov ← gameRepo pov fullId
    g2 ← systemMessages(pov.game, messages)
    g3 = g2.withEvents(!pov.color, List(RedirectEvent(url)))
    _ ← save(pov.game, g3)
    _ ← addEntry(g3, entryData)
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
    g2 ← systemMessages(g1, messages)
    g3 = g2.withEvents(!color, List(ReloadTableEvent()))
    _ ← save(g1, g3)
  } yield ()

  def activity(gameId: String, colorName: String): Int =
    Color(colorName) some { aliveMemo.activity(gameId, _) } none 0

  def room(gameId: String): IO[String] =
    roomRepo room gameId map (_.render)

  def possibleMoves(gameId: String, colorName: String): IO[Map[String, Any]] =
    for {
      color ← ioColor(colorName)
      game ← gameRepo game gameId
    } yield game.toChess.situation.destinations map {
      case (from, dests) ⇒ from.key -> (dests.mkString)
    } toMap

  private def systemMessages(game: DbGame, messageString: String): IO[DbGame] =
    if (game.invited.isHuman) {
      val messages = (messageString split '$').toList
      roomRepo.addSystemMessages(game.id, messages) map { _ ⇒
        game withEvents (messages map { msg ⇒ MessageEvent("system", msg) })
      }
    }
    else io(game)
}
