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

  private implicit val timeout = Timeout(300 millis)
  private implicit val executor = Akka.system.dispatcher

  def show(fullId: String): Future[IO[Valid[Map[String, Any]]]] =
    (gameHubMemo getFromFullId fullId) ? game.GetVersion map {
      case game.Version(version) ⇒ for {
        povOption ← gameRepo pov fullId
        gameInfo ← povOption.fold(
          pov ⇒ messenger render pov.game.id map { roomHtml ⇒
            Map(
              "version" -> version,
              "roomHtml" -> roomHtml,
              "possibleMoves" -> {
                if (pov.game playableBy pov.player)
                  pov.game.toChess.situation.destinations map {
                    case (from, dests) ⇒ from.key -> (dests.mkString)
                  } toMap
                else null
              }
            ).success
          },
          io(GameNotFound)
        )
      } yield gameInfo
    }

  def join(
    fullId: String,
    url: String,
    messages: String,
    entryData: String): IO[Valid[Unit]] = for {
    povOption ← gameRepo pov fullId
    op ← povOption.fold(
      pov ⇒ for {
        p1 ← starter.start(pov.game, entryData)
        p2 ← messenger.systemMessages(p1.game, messages) map { evts ⇒
          p1 + RedirectEvent(!pov.color, url) ++ evts
        }
        _ ← gameRepo save p2
        _ ← gameSocket send p2
      } yield success(),
      io(GameNotFound)
    )
  } yield op

  def start(gameId: String, entryData: String): IO[Valid[Unit]] =
    gameRepo game gameId flatMap { gameOption ⇒
      gameOption.fold(
        g1 ⇒ for {
          progress ← starter.start(g1, entryData)
          _ ← gameRepo save progress
          _ ← gameSocket send progress
        } yield success(Unit),
        io { !!("No such game") }
      )
    }

  def rematchAccept(
    gameId: String,
    newGameId: String,
    colorName: String,
    whiteRedirect: String,
    blackRedirect: String,
    entryData: String,
    messageString: String): IO[Valid[Unit]] = for {
    color ← ioColor(colorName)
    newGameOption ← gameRepo game newGameId
    g1Option ← gameRepo game gameId
    result ← (newGameOption |@| g1Option).tupled.fold(
      games ⇒ {
        val (newGame, g1) = games
        val progress = Progress(g1, List(
          RedirectEvent(White, whiteRedirect),
          RedirectEvent(Black, blackRedirect),
          // tell spectators to reload the table
          ReloadTableEvent(White),
          ReloadTableEvent(Black)))
        for {
          _ ← gameRepo save progress
          _ ← gameSocket send progress
          newProgress ← starter.start(newGame, entryData)
          newProgress2 ← messenger.systemMessages(
            newProgress.game, messageString
          ) map newProgress.++
          _ ← gameRepo save newProgress2
          _ ← gameSocket send newProgress2
        } yield success()
      },
      io(GameNotFound)
    ): IO[Valid[Unit]]
  } yield result

  def reloadTable(gameId: String): IO[Valid[Unit]] = for {
    g1Option ← gameRepo game gameId
    result ← g1Option.fold(
      g1 ⇒ {
        val progress = Progress(g1, Color.all map ReloadTableEvent)
        for {
          _ ← gameRepo save progress
          _ ← gameSocket send progress
        } yield success()
      },
      io(GameNotFound)
    )
  } yield result

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
