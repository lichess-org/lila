package lila.game

import chess.Clock

import lila.common.LightUser
import play.twirl.api.Html

object Namer {

  def players(game: Game, withRatings: Boolean = true)(implicit lightUser: LightUser.Getter): (Html, Html) =
    player(game.firstPlayer, withRatings) -> player(game.secondPlayer, withRatings)

  def player(p: Player, withRating: Boolean = true, withTitle: Boolean = true)(implicit lightUser: LightUser.Getter) = Html {
    p.aiLevel.fold(
      p.userId.flatMap(lightUser).fold(lila.user.User.anonymous) { user =>
        if (withRating) s"${withTitle.fold(user.titleNameHtml, user.name)}&nbsp;(${ratingString(p)})"
        else withTitle.fold(user.titleName, user.name)
      }) { level => s"A.I. level $level" }
  }

  def playerText(player: Player, withRating: Boolean = false)(implicit lightUser: LightUser.Getter): String =
    player.aiLevel.fold(
      player.userId.flatMap(lightUser).fold(player.name | "Anon.") { u =>
        player.rating.ifTrue(withRating).fold(u.titleName) { r => s"${u.titleName} ($r)" }
      }
    ) { level => s"A.I. level $level" }

  def gameVsText(game: Game, withRatings: Boolean = false)(implicit lightUser: LightUser.Getter): String =
    s"${playerText(game.whitePlayer, withRatings)} vs ${playerText(game.blackPlayer, withRatings)}"

  private def ratingString(p: Player) = p.rating match {
    case Some(rating) => s"$rating${if (p.provisional) "?" else ""}"
    case _            => "?"
  }

  def playerString(p: Player, withRating: Boolean = true, withTitle: Boolean = true)(implicit lightUser: String => Option[LightUser]) =
    player(p, withRating, withTitle)(lightUser).body.replace("&nbsp;", " ")
}
