package lila.game

import chess.Clock

import lila.common.LightUser
import play.api.templates.Html

object Namer {

  def players(game: Game, withRatings: Boolean = true)(implicit lightUser: String => Option[LightUser]): (Html, Html) =
    player(game.firstPlayer, withRatings) -> player(game.secondPlayer, withRatings)

  def player(player: Player, withRating: Boolean = true)(implicit lightUser: String => Option[LightUser]) = Html {
    player.aiLevel.fold(
      player.userId.flatMap(lightUser).fold(lila.user.User.anonymous) { user =>
        withRating.fold(
          s"${user.titleNameHtml}&nbsp;(${player.rating getOrElse "?"})",
          user.titleName)
      }) { level => s"A.I.&nbsp;level&nbsp;$level" }
  }

  def shortClock(clock: Clock): Html = Html {
    s"${clock.limitInMinutes}&nbsp;+&nbsp;${clock.increment}"
  }
}
