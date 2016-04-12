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
        if (withRating) s"${withTitle.fold(user.titleNameHtml, user.name)}&nbsp;(${ratingString(p)})"
        else withTitle.fold(user.titleName, user.name)
      }) { level => s"A.I. level $level" }
  }

  private def ratingString(p: Player) = p.rating match {
    case Some(rating) => s"$rating${if (p.provisional) "?" else ""}"
    case _            => "?"
  }

  def playerString(p: Player, withRating: Boolean = true, withTitle: Boolean = true)(implicit lightUser: String => Option[LightUser]) =
    player(p, withRating, withTitle)(lightUser).body.replace("&nbsp;", " ")
}
