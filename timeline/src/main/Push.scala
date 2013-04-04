package lila.timeline

import chess.Color, Color._
import lila.game.{ Game, Namer }
import lila.db.api.$insert
import lila.hub.actorApi.lobby.TimelineEntry
import tube.entryTube

import akka.actor.ActorRef
import play.api.libs.concurrent.Execution.Implicits._

final class Push(
    lobby: ActorRef,
    getUsername: String ⇒ Fu[String]) {

  def apply(game: Game): Funit = makeEntry(game) flatMap { entry ⇒
    $insert(entry) >> (lobby ! TimelineEntry(entry.render))
  }

  private def makeEntry(game: Game): Fu[Entry] =
    usernameElo(game, White) zip usernameElo(game, Black) map {
      case (whiteName, blackName) ⇒ Entry(
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
