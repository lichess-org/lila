package lila.game

import chess.Clock

import lila.common.LightUser
import play.api.templates.Html

object Namer {

  def players(game: Game, withRatings: Boolean = true)(implicit lightUser: String => Option[LightUser]): (Html, Html) =
    player(game.firstPlayer, withRatings) -> player(game.secondPlayer, withRatings)

  def player(player: Player, withRating: Boolean = true, withTitle: Boolean = true)(implicit lightUser: String => Option[LightUser]) = Html {
    player.aiLevel.fold(
      player.userId.flatMap(lightUser).fold(lila.user.User.anonymous) { user =>
        withRating.fold(
          s"${withTitle.fold(user.titleNameHtml, user.name)}&nbsp;(${player.rating getOrElse "?"})",
          withTitle.fold(user.titleName, user.name))
      }) { level => s"A.I.&nbsp;level&nbsp;$level" }
  }

  def shortClock(clock: Clock): Html = Html {
    s"${clock.limitInMinutes}&nbsp;+&nbsp;${clock.increment}"
  }
}
