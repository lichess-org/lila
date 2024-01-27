package lila.game

import lila.common.LightUser

object Namer {

  def playerTextBlocking(player: Player, withRating: Boolean = false)(implicit
      lightUser: LightUser.GetterSync
  ): String =
    playerTextUser(player, player.userId flatMap lightUser, withRating)

  def playerText(player: Player, withRating: Boolean = false)(implicit
      lightUser: LightUser.Getter
  ): Fu[String] =
    player.userId.??(lightUser) dmap {
      playerTextUser(player, _, withRating)
    }

  private def playerTextUser(player: Player, user: Option[LightUser], withRating: Boolean): String =
    player.engineConfig.fold(
      user.fold(player.name | "Anon.") { u =>
        player.rating.ifTrue(withRating).fold(u.titleName) { r =>
          s"${u.titleName} ($r)"
        }
      }
    ) { ec =>
      s"${ec.engine.fullName} level ${ec.level}}" // rework to use i18n?
    }

  def gameVsTextBlocking(game: Game, withRatings: Boolean = false)(implicit
      lightUser: LightUser.GetterSync
  ): String =
    s"${playerTextBlocking(game.sentePlayer, withRatings)} - ${playerTextBlocking(game.gotePlayer, withRatings)}"

  def gameVsText(game: Game, withRatings: Boolean = false)(implicit lightUser: LightUser.Getter): Fu[String] =
    game.sentePlayer.userId.??(lightUser) zip
      game.gotePlayer.userId.??(lightUser) dmap { case (wu, bu) =>
        s"${playerTextUser(game.sentePlayer, wu, withRatings)} - ${playerTextUser(game.gotePlayer, bu, withRatings)}"
      }

  def ratingString(p: Player) =
    p.rating match {
      case Some(rating) => s"$rating${if (p.provisional) "?" else ""}"
      case _            => "?"
    }
}
