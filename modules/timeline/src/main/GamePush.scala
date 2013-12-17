package lila.timeline

import akka.actor._
import akka.pattern.{ ask, pipe }
import play.api.templates.Html

import chess.Color, Color._
import lila.db.api.$insert
import lila.game.{ Game, Namer }
import lila.hub.actorApi.timeline._
import makeTimeout.short
import tube.gameEntryTube

private[timeline] final class GamePush(
    lobbySocket: ActorSelection,
    renderer: ActorSelection,
    getUsername: String ⇒ Fu[String]) extends Actor {

  def receive = {
    case game: Game ⇒ makeEntry(game) flatMap { entry ⇒
      $insert(entry) >>- {
        renderer ? entry map {
          case view: Html ⇒ GameEntryView(view.body)
        } pipeToSelection lobbySocket
      }
    } logFailure ("[timeline] push " + game.id)
  }

  private def makeEntry(game: Game): Fu[GameEntry] =
    usernameRating(game, White) zip usernameRating(game, Black) map {
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

  private def usernameRating(game: Game, color: Color): Fu[String] =
    Namer.player(game player color)(getUsername)
}
