package lila.game

import chess.Clock

import lila.user.User
import play.api.templates.Html

object Namer {

  def players(game: Game, withRatings: Boolean = true)(implicit getUsername: String => Fu[String]): Fu[(Html, Html)] =
    player(game.firstPlayer, withRatings) zip player(game.secondPlayer, withRatings)

  def player(player: Player, withRating: Boolean = true)(implicit getUsername: String => Fu[String]): Fu[Html] =
    player.aiLevel.fold(
      player.userId.fold(fuccess(User.anonymous)) { id =>
        getUsername(id) map { username =>
          withRating.fold(
            s"$username&nbsp;(${player.rating getOrElse "?"})",
            username)
        }
      }) { level =>
        fuccess("A.I.&nbsp;level&nbsp;" + level)
      } map Html.apply

  def shortClock(clock: Clock): Html = Html {
    s"${clock.limitInMinutes}&nbsp;+&nbsp;${clock.increment}"
  }
}
