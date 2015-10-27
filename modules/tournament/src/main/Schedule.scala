package lila.tournament

import chess.StartingPosition
import chess.variant.Variant
import org.joda.time.DateTime

case class Schedule(
    freq: Schedule.Freq,
    speed: Schedule.Speed,
    variant: Variant,
    position: StartingPosition,
    at: DateTime) {

  def name = freq match {
    case m@Schedule.Freq.ExperimentalMarathon      => m.name
    case _ if variant.standard && position.initial => s"${freq.toString} ${speed.toString}"
    case _ if variant.standard                     => s"${position.shortName} ${speed.toString}"
    case _                                         => s"${freq.toString} ${variant.name}"
  }

  def sameSpeed(other: Schedule) = speed == other.speed

  def sameVariant(other: Schedule) = variant.id == other.variant.id

  def sameFreq(other: Schedule) = freq == other.freq

  def similarTo(other: Schedule) = sameSpeed(other) && sameVariant(other) && sameFreq(other)

  override def toString = s"$freq $variant $speed $at"
}

object Schedule {

  sealed trait Freq {
    val name = toString.toLowerCase
  }
  object Freq {
    case object Hourly extends Freq
    case object Daily extends Freq
    case object Nightly extends Freq // deprecated - use Eastern instead
    case object Eastern extends Freq
    case object Weekly extends Freq
    case object Monthly extends Freq
    case object Marathon extends Freq
    case object ExperimentalMarathon extends Freq { // for DB BC
      override val name = "Experimental Marathon"
    }
    val all: List[Freq] = List(Hourly, Daily, Nightly, Eastern, Weekly, Monthly, Marathon, ExperimentalMarathon)
    def apply(name: String) = all find (_.name == name)
  }

  sealed trait Speed {
    def name = toString.toLowerCase
  }
  object Speed {
    case object Bullet extends Speed
    case object SuperBlitz extends Speed
    case object Blitz extends Speed
    case object Classical extends Speed
    val all: List[Speed] = List(Bullet, SuperBlitz, Blitz, Classical)
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
    import chess.variant._
    Some((sched.freq, sched.speed, sched.variant) match {

      case (Hourly, Bullet, _)                          => 26
      case (Hourly, SuperBlitz, _)                      => 56
      case (Hourly, Blitz, _)                           => 56
      case (Hourly, Classical, _)                       => 116

      case (Daily | Nightly | Eastern, Bullet, _)       => 60
      case (Daily | Nightly | Eastern, SuperBlitz, _)   => 90
      case (Daily | Nightly | Eastern, Blitz, Standard) => 90
      case (Daily | Nightly | Eastern, Blitz, _)        => 60 // variant daily is shorter
      case (Daily | Nightly | Eastern, Classical, _)    => 60 * 2

      case (Weekly, Bullet, _)                          => 90
      case (Weekly, SuperBlitz, _)                      => 60 * 2
      case (Weekly, Blitz, _)                           => 60 * 2
      case (Weekly, Classical, _)                       => 60 * 3

      case (Monthly, Bullet, _)                         => 60 * 2
      case (Monthly, SuperBlitz, _)                     => 60 * 3
      case (Monthly, Blitz, _)                          => 60 * 3
      case (Monthly, Classical, _)                      => 60 * 4

      case (Marathon, _, _)                             => 60 * 24 // lol
      case (ExperimentalMarathon, _, _)                 => 60 * 4

    }) filter (0!=)
  }

  private val blitzIncHours = Set(1, 7, 13, 19)

  private[tournament] def clockFor(sched: Schedule) = sched.speed match {
    case Speed.Bullet                                        => TournamentClock(60, 0)
    case Speed.SuperBlitz                                    => TournamentClock(3 * 60, 0)
    case Speed.Blitz if blitzIncHours(sched.at.getHourOfDay) => TournamentClock(3 * 60, 2)
    case Speed.Blitz                                         => TournamentClock(5 * 60, 0)
    case Speed.Classical                                     => TournamentClock(10 * 60, 0)
  }
}
