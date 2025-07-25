package lila.core
package challenge

import _root_.chess.variant.Variant
import _root_.chess.{ Color, Rated }
import _root_.chess.rating.RatingProvisional
import _root_.chess.IntRating
import scalalib.model.Days

import lila.core.id.ChallengeId
import lila.core.userId.UserId

trait Challenge:
  import Challenge.*
  val id: ChallengeId
  val variant: Variant
  val rated: Rated
  val timeControl: TimeControl
  val finalColor: Color
  val destUser: Option[Challenger.Registered]
  val challenger: Challenger
  def destUserId = destUser.map(_.id)
  def challengerUser = challenger match
    case u: Challenger.Registered => u.some
    case _ => none
  def challengerIsAnon = challenger.isInstanceOf[Challenger.Anonymous]
  def clock = timeControl match
    case c: Challenge.TimeControl.Clock => c.some
    case _ => none

object Challenge:

  sealed trait TimeControl:
    def realTime: Option[_root_.chess.Clock.Config] = none
  object TimeControl:
    case object Unlimited extends TimeControl
    case class Correspondence(days: Days) extends TimeControl
    case class Clock(config: _root_.chess.Clock.Config) extends TimeControl:
      override def realTime = config.some
      // All durations are expressed in seconds
      export config.{ limit, increment, show }

  case class Rating(int: IntRating, provisional: RatingProvisional):
    def show = s"$int${if provisional.yes then "?" else ""}"

  enum Challenger:
    case Registered(id: UserId, rating: Rating)
    case Anonymous(secret: String)
    case Open

// name is weird, but `Decline` and `Cancel`
// would be a pain to move in core
enum PositiveEvent:
  case Create(c: Challenge)
  case Accept(c: Challenge, joinerId: Option[UserId])
