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

  def similarSpeed(other: Schedule) = Schedule.Speed.similar(speed, other.speed)

  def sameVariant(other: Schedule) = variant.id == other.variant.id

  def sameFreq(other: Schedule) = freq == other.freq

  def similarTo(other: Schedule) = similarSpeed(other) && sameVariant(other) && sameFreq(other)

  override def toString = s"$freq $variant $speed $at"
}

object Schedule {

  sealed abstract class Freq(val id: Int, val importance: Int) extends Ordered[Freq] {

    val name = toString.toLowerCase

    def compare(other: Freq) = importance compare other.importance
  }
  object Freq {
    case object Hourly extends Freq(10, 10)
    case object Daily extends Freq(20, 20)
    case object Eastern extends Freq(30, 15)
    case object Weekly extends Freq(40, 40)
    case object Monthly extends Freq(50, 50)
    case object Marathon extends Freq(60, 60)
    case object ExperimentalMarathon extends Freq(61, 55) { // for DB BC
      override val name = "Experimental Marathon"
    }
    case object Unique extends Freq(90, 59)
    val all: List[Freq] = List(Hourly, Daily, Eastern, Weekly, Monthly, Marathon, ExperimentalMarathon, Unique)
    def apply(name: String) = all find (_.name == name)
    def byId(id: Int) = all find (_.id == id)
  }

  sealed abstract class Speed(val id: Int) {
    def name = toString.toLowerCase
  }
  object Speed {
    case object HyperBullet extends Speed(10)
    case object Bullet extends Speed(20)
    case object SuperBlitz extends Speed(30)
    case object Blitz extends Speed(40)
    case object Classical extends Speed(50)
    val all: List[Speed] = List(HyperBullet, Bullet, SuperBlitz, Blitz, Classical)
    val mostPopular: List[Speed] = List(Bullet, Blitz, Classical)
    def apply(name: String) = all find (_.name == name)
    def byId(id: Int) = all find (_.id == id)
    def similar(s1: Speed, s2: Speed) = (s1, s2) match {
      case (a, b) if a == b      => true
      case (HyperBullet, Bullet) => true
      case (Bullet, HyperBullet) => true
      case _                     => false
    }
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

      case (Hourly, HyperBullet | Bullet, _)          => 26
      case (Hourly, SuperBlitz, _)                    => 56
      case (Hourly, Blitz, _)                         => 56
      case (Hourly, Classical, _)                     => 116

      case (Daily | Eastern, HyperBullet | Bullet, _) => 60
      case (Daily | Eastern, SuperBlitz, _)           => 90
      case (Daily | Eastern, Blitz, Standard)         => 120
      case (Daily | Eastern, Classical, _)            => 150

      case (Daily | Eastern, Blitz, Crazyhouse)       => 120
      case (Daily | Eastern, Blitz, _)                => 60 // variant daily is shorter

      case (Weekly, HyperBullet | Bullet, _)          => 60 * 2
      case (Weekly, SuperBlitz, _)                    => 60 * 2 + 30
      case (Weekly, Blitz, _)                         => 60 * 3
      case (Weekly, Classical, _)                     => 60 * 4

      case (Monthly, HyperBullet | Bullet, _)         => 60 * 3
      case (Monthly, SuperBlitz, _)                   => 60 * 3 + 30
      case (Monthly, Blitz, _)                        => 60 * 4
      case (Monthly, Classical, _)                    => 60 * 5

      case (Marathon, _, _)                           => 60 * 24 // lol
      case (ExperimentalMarathon, _, _)               => 60 * 4

      case (Unique, _, _)                             => 0

    }) filter (0!=)
  }

  private val blitzIncHours = Set(1, 7, 13, 19)
  private def makeInc(sched: Schedule) =
    sched.freq == Freq.Hourly && blitzIncHours(sched.at.getHourOfDay)

  private[tournament] def clockFor(sched: Schedule) = sched.speed match {
    case Speed.HyperBullet             => TournamentClock(30, 0)
    case Speed.Bullet                  => TournamentClock(60, 0)
    case Speed.SuperBlitz              => TournamentClock(3 * 60, 0)
    case Speed.Blitz if makeInc(sched) => TournamentClock(3 * 60, 2)
    case Speed.Blitz                   => TournamentClock(5 * 60, 0)
    case Speed.Classical               => TournamentClock(10 * 60, 0)
  }
}
