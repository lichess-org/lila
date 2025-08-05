package lila.game

import lila.core.LightUser
import lila.core.game.{ Game, Player }

object Namer extends lila.core.game.Namer:

  def playerTextBlocking(player: Player, withRating: Boolean = false)(using
      lightUser: LightUser.GetterSync
  ): String =
    playerTextUser(player, player.userId.flatMap(lightUser), withRating)

  def playerText(player: Player, withRating: Boolean = false)(using lightUser: LightUser.Getter): Fu[String] =
    player.userId
      .so(lightUser)
      .dmap:
        playerTextUser(player, _, withRating)

  private def playerTextUser(player: Player, user: Option[LightUser], withRating: Boolean): String =
    player.aiLevel match
      case Some(level) => s"Stockfish level $level"
      case None =>
        user.fold(player.name.fold("Anon.")(_.value)): u =>
          ratingString(player)
            .ifTrue(withRating)
            .fold(u.titleName): rating =>
              s"${u.titleName} ($rating)"

  def gameVsTextBlocking(game: Game, withRatings: Boolean = false)(using
      lightUser: LightUser.GetterSync
  ): String =
    s"${playerTextBlocking(game.whitePlayer, withRatings)} - ${playerTextBlocking(game.blackPlayer, withRatings)}"

  def gameVsText(game: Game, withRatings: Boolean = false)(using lightUser: LightUser.Getter): Fu[String] =
    game.whitePlayer.userId.so(lightUser).zip(game.blackPlayer.userId.so(lightUser)).dmap { (wu, bu) =>
      s"${playerTextUser(game.whitePlayer, wu, withRatings)} - ${playerTextUser(game.blackPlayer, bu, withRatings)}"
    }

  def ratingString(p: Player): Option[String] =
    p.rating.map: rating =>
      s"$rating${p.provisional.yes.so("?")}"
