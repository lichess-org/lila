package lila.game

import lila.common.LightUser
import lila.user.User
import play.twirl.api.Html

object Namer {

  def players(game: Game, withRatings: Boolean = true)(implicit lightUser: LightUser.GetterSync): (Html, Html) =
    player(game.firstPlayer, withRatings) -> player(game.secondPlayer, withRatings)

  def player(p: Player, withRating: Boolean = true, withTitle: Boolean = true)(implicit lightUser: LightUser.GetterSync) = Html {
    p.aiLevel.fold(
      p.userId.flatMap(lightUser).fold(lila.user.User.anonymous) { user =>
        val title = (user.title ifTrue withTitle) ?? { t =>
          s"""<span class="title" title="${User titleName t}">$t</span>&nbsp;"""
        }
        if (withRating) s"$title${user.name}&nbsp;(${ratingString(p)})"
        else s"$title${user.name}"
      }
    ) { level => s"A.I. level $level" }
  }

  def playerText(player: Player, withRating: Boolean = false)(implicit lightUser: LightUser.GetterSync): String =
    player.aiLevel.fold(
      player.userId.flatMap(lightUser).fold(player.name | "Anon.") { u =>
        player.rating.ifTrue(withRating).fold(u.titleName) { r => s"${u.titleName} ($r)" }
      }
    ) { level => s"A.I. level $level" }

  def gameVsText(game: Game, withRatings: Boolean = false)(implicit lightUser: LightUser.GetterSync): String =
    s"${playerText(game.whitePlayer, withRatings)} - ${playerText(game.blackPlayer, withRatings)}"

  private def ratingString(p: Player) = p.rating match {
    case Some(rating) => s"$rating${if (p.provisional) "?" else ""}"
    case _ => "?"
  }

  def playerString(p: Player, withRating: Boolean = true, withTitle: Boolean = true)(implicit lightUser: LightUser.GetterSync) =
    player(p, withRating, withTitle)(lightUser).body.replace("&nbsp;", " ")
}
