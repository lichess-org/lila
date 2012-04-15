package lila

import model._
import memo._
import db.{ GameRepo, RoomRepo }
import chess.{ Color, White, Black }
import game.IsConnected

import scalaz.effects._
import akka.pattern.ask
import akka.dispatch.{ Future, Promise }
import akka.util.duration._
import akka.util.Timeout
import play.api.libs.concurrent._
import play.api.Play.current

final class AppApi(
    gameRepo: GameRepo,
    gameSocket: game.Socket,
    gameHubMemo: game.HubMemo,
    messenger: Messenger,
    starter: Starter) {

  private implicit val timeout = Timeout(200 millis)
  private implicit val executor = Akka.system.dispatcher

  def show(fullId: String): Future[IO[Map[String, Any]]] =
    (gameHubMemo getFromFullId fullId) ? game.GetVersion map {
      case game.Version(version) ⇒ for {
        pov ← gameRepo pov fullId
        roomHtml ← messenger render pov.game.id
      } yield Map(
        "version" -> version,
        "roomHtml" -> roomHtml,
        "possibleMoves" -> {
          if (pov.game playableBy pov.player)
            pov.game.toChess.situation.destinations map {
              case (from, dests) ⇒ from.key -> (dests.mkString)
            } toMap
          else null
        }
      )
    }

  def join(
    fullId: String,
    url: String,
    messages: String,
    entryData: String): IO[Unit] = for {
    pov ← gameRepo pov fullId
    p1 ← starter.start(pov.game, entryData)
    p2 ← messenger.systemMessages(p1.game, messages) map { evts ⇒
      p1 + RedirectEvent(!pov.color, url) ++ evts
    }
    _ ← gameRepo save p2
    _ ← gameSocket send p2
  } yield ()

  def start(gameId: String, entryData: String): IO[Unit] = for {
    g1 ← gameRepo game gameId
    progress ← starter.start(g1, entryData)
    _ ← gameRepo save progress
    _ ← gameSocket send progress
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
    progress = Progress(g1, List(
      RedirectEvent(White, whiteRedirect),
      RedirectEvent(Black, blackRedirect),
      // to tell spectators to reload the table
      ReloadTableEvent(White),
      ReloadTableEvent(Black)))
    _ ← gameRepo save progress
    _ ← gameSocket send progress
    newProgress ← starter.start(newGame, entryData)
    newProgress2 ← messenger.systemMessages(
      newProgress.game, messageString
    ) map newProgress.++
    _ ← gameRepo save newProgress2
    _ ← gameSocket send newProgress2
  } yield ()

  def reloadTable(gameId: String): IO[Unit] = for {
    g1 ← gameRepo game gameId
    progress = Progress(g1, Color.all map ReloadTableEvent)
    _ ← gameRepo save progress
    _ ← gameSocket send progress
  } yield ()

  def gameVersion(gameId: String): Future[Int] =
    (gameHubMemo get gameId) ? game.GetVersion map {
      case game.Version(v) ⇒ v
    }

  def isConnected(gameId: String, colorName: String): Future[Boolean] =
    Color(colorName).fold(
      c ⇒ (gameHubMemo get gameId) ? IsConnected(c) mapTo manifest[Boolean],
      Promise successful false)

  private def ioColor(colorName: String): IO[Color] = io {
    Color(colorName) err "Invalid color"
  }
}
