package lila.core
package game

import _root_.chess.{ Color, PlayerName, Ply }
import cats.kernel.Eq

import lila.core.id.GamePlayerId
import lila.core.perf.Perf
import lila.core.rating.data.{ IntRating, IntRatingDiff, RatingProvisional }
import lila.core.user.WithPerf
import lila.core.userId.{ UserId, UserIdOf }

case class Player(
    id: GamePlayerId,
    color: Color,
    aiLevel: Option[Int],
    isWinner: Option[Boolean] = None,
    isOfferingDraw: Boolean = false,
    proposeTakebackAt: Ply = Ply.initial, // ply when takeback was proposed
    userId: Option[UserId] = None,
    rating: Option[IntRating] = None,
    ratingDiff: Option[IntRatingDiff] = None,
    provisional: RatingProvisional = RatingProvisional.No,
    blurs: Blurs = Blurs(0L),
    berserk: Boolean = false,
    blindfold: Boolean = false,
    name: Option[PlayerName] = None
):
  def isAi = aiLevel.isDefined

  def hasUser = userId.isDefined

  def isUser[U: UserIdOf](u: U) = userId.has(u.id)

  def removeTakebackProposition = copy(proposeTakebackAt = Ply.initial)

  def isProposingTakeback = proposeTakebackAt > 0

  def before(other: Player) =
    ((rating, id), (other.rating, other.id)) match
      case ((Some(a), _), (Some(b), _)) if a != b => a.value > b.value
      case ((Some(_), _), (None, _))              => true
      case ((None, _), (Some(_), _))              => false
      case ((_, a), (_, b))                       => a.value < b.value

  def ratingAfter = rating.map(_.applyDiff(~ratingDiff))

  def stableRating = rating.ifFalse(provisional.value)

  def stableRatingAfter = stableRating.map(_.applyDiff(~ratingDiff))

  def light = LightPlayer(color, aiLevel, userId, rating, ratingDiff, provisional, berserk)

object Player:
  given Eq[Player] = Eq.by(p => (p.id, p.userId))

trait NewPlayer:
  def apply(color: Color, user: Option[WithPerf]): Player
  def apply(color: Color, userId: UserId, rating: IntRating, provisional: RatingProvisional): Player
  def apply(color: Color, userPerf: (UserId, Perf)): Player
  def anon(color: Color, aiLevel: Option[Int] = None): Player
