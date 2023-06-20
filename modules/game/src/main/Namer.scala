package lila.game

import lila.common.LightUser

object Namer:

  def playerTextBlocking(player: Player, withRating: Boolean = false)(using
      lightUser: LightUser.GetterSync
  ): String =
    playerTextUser(player, player.userId flatMap lightUser, withRating)

  def playerText(player: Player, withRating: Boolean = false)(using lightUser: LightUser.Getter): Fu[String] =
    player.userId.so(lightUser) dmap {
      playerTextUser(player, _, withRating)
    }

  def playerTextUser(player: Player, user: Option[LightUser], withRating: Boolean = false): String =
    player.aiLevel.fold(
      user.fold(player.name | "Anon.") { u =>
        player.rating.ifTrue(withRating).fold(u.titleName) { r =>
          s"${u.titleName} ($r)"
        }
      }
    ) { level =>
      s"Stockfish level $level"
    }

  def gameVsTextBlocking(game: Game, withRatings: Boolean = false)(using
      lightUser: LightUser.GetterSync
  ): String =
    s"${playerTextBlocking(game.whitePlayer, withRatings)} - ${playerTextBlocking(game.blackPlayer, withRatings)}"

  def gameVsText(game: Game, withRatings: Boolean = false)(using lightUser: LightUser.Getter): Fu[String] =
    game.whitePlayer.userId.so(lightUser) zip
      game.blackPlayer.userId.so(lightUser) dmap { (wu, bu) =>
        s"${playerTextUser(game.whitePlayer, wu, withRatings)} - ${playerTextUser(game.blackPlayer, bu, withRatings)}"
      }

  def ratingString(p: Player): Option[String] =
    p.rating.map { rating =>
      s"$rating${p.provisional.yes so "?"}"
    }
