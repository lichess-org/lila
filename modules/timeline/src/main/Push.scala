package lila.timeline

import chess.Color, Color._
import lila.game.{ Game, Namer }
import lila.db.api.$insert
import lila.hub.actorApi.lobby.TimelineGameEntry
import tube.gameEntryTube
import makeTimeout.short

import play.api.templates.Html
import akka.actor._
import akka.pattern.{ ask, pipe }

private[timeline] final class PushGame(
    lobbySocket: lila.hub.ActorLazyRef,
    renderer: lila.hub.ActorLazyRef,
    getUsername: String ⇒ Fu[String]) extends Actor {

  def receive = {
    case game: Game ⇒ makeEntry(game) flatMap { entry ⇒
      $insert(entry) >>- {
        renderer ? entry map {
          case view: Html ⇒ TimelineGameEntry(view.body)
        } pipeTo lobbySocket.ref
      }
    } logFailure ("[timeline] push " + game.id)
  }

  private def makeEntry(game: Game): Fu[GameEntry] =
    usernameElo(game, White) zip usernameElo(game, Black) map {
      case (whiteName, blackName) ⇒ GameEntry(
        gameId = game.id,
        whiteName = whiteName,
        blackName = blackName,
        whiteId = userId(game, White),
        blackId = userId(game, Black),
        variant = game.variant.name,
        rated = game.rated,
        clock = game.clock map (_.show))
    }

  private def userId(game: Game, color: Color): Option[String] =
    (game player color).userId

  private def usernameElo(game: Game, color: Color): Fu[String] =
    Namer.player(game player color)(getUsername)
}
