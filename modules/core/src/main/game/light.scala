package lila.core
package game

import _root_.chess.{ Color, Status, IntRating }
import _root_.chess.variant.Variant
import _root_.chess.rating.{ IntRatingDiff, RatingProvisional }

import lila.core.id.GameId
import lila.core.userId.UserId

case class LightGame(
    id: GameId,
    whitePlayer: LightPlayer,
    blackPlayer: LightPlayer,
    status: Status,
    win: Option[Color],
    variant: Variant
):
  def playable                                            = status < Status.Aborted
  def player(color: Color): LightPlayer                   = color.fold(whitePlayer, blackPlayer)
  def players                                             = List(whitePlayer, blackPlayer)
  def playerByUserId(userId: UserId): Option[LightPlayer] = players.find(_.userId contains userId)
  def finished                                            = status >= Status.Mate
  def winner: Option[LightPlayer]                         = win.map(_.fold(whitePlayer, blackPlayer))
  def winnerUserId: Option[UserId]                        = winner.flatMap(_.userId)

case class LightPlayer(
    color: Color,
    aiLevel: Option[Int],
    userId: Option[UserId] = None,
    rating: Option[IntRating] = None,
    ratingDiff: Option[IntRatingDiff] = None,
    provisional: RatingProvisional = RatingProvisional.No,
    berserk: Boolean = false
)

case class LightPov(game: LightGame, color: Color):
  export game.id as gameId
  def player   = game.player(color)
  def opponent = game.player(!color)

object LightPov:
  def apply(game: LightGame, userId: UserId): Option[LightPov] =
    game.playerByUserId(userId).map { p => LightPov(game, p.color) }

trait GameLightRepo:
  def gamesFromSecondary(gameIds: Seq[GameId]): Fu[List[LightGame]]
  def gamesFromPrimary(gameIds: Seq[GameId]): Fu[List[LightGame]]
