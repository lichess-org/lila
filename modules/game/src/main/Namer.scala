package lila.game

import chess.Clock

import lila.common.LightUser
import play.twirl.api.Html

object Namer {

  def players(game: Game, withRatings: Boolean = true)(implicit lightUser: String => Option[LightUser]): (Html, Html) =
    player(game.firstPlayer, withRatings) -> player(game.secondPlayer, withRatings)

  def player(p: Player, withRating: Boolean = true, withTitle: Boolean = true)(implicit lightUser: String => Option[LightUser]) = Html {
    p.aiLevel.fold(
      p.userId.flatMap(lightUser).fold(lila.user.User.anonymous) { user =>
        withRating.fold(
          s"${withTitle.fold(user.titleNameHtml, user.name)}&nbsp;(${p.rating getOrElse "?"})",
          withTitle.fold(user.titleName, user.name))
      }) { level => s"A.I.&nbsp;level&nbsp;$level" }
  }

  def playerString(p: Player, withRating: Boolean = true, withTitle: Boolean = true)(implicit lightUser: String => Option[LightUser]) =
    player(p, withRating, withTitle)(lightUser).body.replace("&nbsp;", " ")

  def shortClock(clock: Clock): Html = Html {
    s"${clock.limitInMinutes}&nbsp;+&nbsp;${clock.increment}"
  }
}
