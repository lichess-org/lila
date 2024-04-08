package lila.core
package challenge

trait Challenge:
  import Challenge.*
  val id: Id
  val variant: chess.variant.Variant
  val mode: chess.Mode
  val timeControl: TimeControl
  val finalColor: chess.Color
  val destUser: Option[Challenger.Registered]
  val challenger: Challenger
  def challengerUser = challenger match
    case u: Challenger.Registered => u.some
    case _                        => none
  def clock = timeControl match
    case c: Challenge.TimeControl.Clock => c.some
    case _                              => none

object Challenge:
  opaque type Id = String
  object Id extends OpaqueString[Id]

  sealed trait TimeControl:
    def realTime: Option[chess.Clock.Config] = none
  object TimeControl:
    case object Unlimited                 extends TimeControl
    case class Correspondence(days: Days) extends TimeControl
    case class Clock(config: chess.Clock.Config) extends TimeControl:
      override def realTime = config.some
      // All durations are expressed in seconds
      export config.{ limit, increment, show }

  case class Rating(int: IntRating, provisional: RatingProvisional):
    def show = s"$int${if provisional.yes then "?" else ""}"

  enum Challenger:
    case Registered(id: UserId, rating: Rating)
    case Anonymous(secret: String)
    case Open

object Event:
  case class Create(c: Challenge)
  case class Accept(c: Challenge, joinerId: Option[UserId])
