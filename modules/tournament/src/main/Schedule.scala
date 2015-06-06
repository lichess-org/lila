package lila.tournament

import chess.variant.Variant
import org.joda.time.DateTime

case class Schedule(
    freq: Schedule.Freq,
    speed: Schedule.Speed,
    variant: Variant,
    at: DateTime) {

  def name =
    if (variant.standard) s"${freq.toString} ${speed.toString}"
    else s"${freq.toString} ${variant.name}"

  def sameSpeed(other: Schedule) = speed == other.speed

  def sameVariant(other: Schedule) = variant.id == other.variant.id
}

object Schedule {

  sealed trait Freq {
    def name = toString.toLowerCase
  }
  object Freq {
    case object Hourly extends Freq
    case object Daily extends Freq
    case object Nightly extends Freq
    case object Weekly extends Freq
    case object Monthly extends Freq
    case object Marathon extends Freq
    val all: List[Freq] = List(Hourly, Daily, Nightly, Weekly, Monthly, Marathon)
    def apply(name: String) = all find (_.name == name)
  }

  sealed trait Speed {
    def name = toString.toLowerCase
  }
  object Speed {
    case object Lightning extends Speed
    case object Bullet extends Speed
    case object SuperBlitz extends Speed
    case object Blitz extends Speed
    case object Classical extends Speed
    val all: List[Speed] = List(Lightning, Bullet, SuperBlitz, Blitz, Classical)
    val mostPopular: List[Speed] = List(Bullet, Blitz, Classical)
    def apply(name: String) = all find (_.name == name)
  }

  sealed trait Season
  object Season {
    case object Spring extends Season
    case object Summer extends Season
    case object Autumn extends Season
    case object Winter extends Season
  }

  private[tournament] def durationFor(sched: Schedule): Option[Int] = {
    import Freq._, Speed._
    Some((sched.freq, sched.speed, sched.variant) match {

      case (Hourly, Lightning, _)           => 20
      case (Hourly, Bullet, _)              => 30
      case (Hourly, SuperBlitz, _)          => 50
      case (Hourly, Blitz, _)               => 60
      case (Hourly, Classical, _)           => 0 // N/A

      case (Daily | Nightly, Lightning, _)  => 0 // N/A
      case (Daily | Nightly, Bullet, _)     => 60
      case (Daily | Nightly, SuperBlitz, _) => 90
      case (Daily | Nightly, Blitz, _)      => 90
      case (Daily | Nightly, Classical, _)  => 60 * 2

      case (Weekly, Lightning, _)           => 0 // N/A
      case (Weekly, Bullet, _)              => 90
      case (Weekly, SuperBlitz, _)          => 60 * 2
      case (Weekly, Blitz, _)               => 60 * 2
      case (Weekly, Classical, _)           => 60 * 3

      case (Monthly, Lightning, _)          => 0 // N/A
      case (Monthly, Bullet, _)             => 60 * 2
      case (Monthly, SuperBlitz, _)         => 60 * 3
      case (Monthly, Blitz, _)              => 60 * 3
      case (Monthly, Classical, _)          => 60 * 4

      case (Marathon, _, _)                 => 60 * 24 // lol

    }) filter (0!=)
  }

  private[tournament] def clockFor(sched: Schedule) = sched.speed match {
    case Speed.Lightning  => TournamentClock(0, 1)
    case Speed.Bullet     => TournamentClock(60, 0)
    case Speed.SuperBlitz => TournamentClock(3 * 60, 0)
    case Speed.Blitz      => TournamentClock(5 * 60, 0)
    case Speed.Classical  => TournamentClock(10 * 60, 0)
  }
}
