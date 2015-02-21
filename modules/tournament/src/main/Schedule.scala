package lila.tournament

import org.joda.time.DateTime

case class Schedule(
    freq: Schedule.Freq,
    speed: Schedule.Speed,
    at: DateTime) {

  def inSeconds: Int = (at.getSeconds - nowSeconds).toInt

  def name = s"${freq.toString} ${speed.toString}"

  def sameSpeed(other: Schedule) = speed == other.speed
}

object Schedule {

  sealed trait Freq {
    def name = toString.toLowerCase
  }
  object Freq {
    case object Hourly extends Freq
    case object Daily extends Freq
    case object Weekly extends Freq
    case object Monthly extends Freq
    val all: List[Freq] = List(Hourly, Daily, Weekly, Monthly)
    def apply(name: String) = all find (_.name == name)
  }

  sealed trait Speed {
    def name = toString.toLowerCase
  }
  object Speed {
    case object Bullet extends Speed
    case object Bullitz extends Speed
    case object Blitz extends Speed
    case object Classical extends Speed
    val all: List[Speed] = List(Bullet, Bullitz, Blitz, Classical)
    def apply(name: String) = all find (_.name == name)
  }

  private[tournament] def durationFor(sched: Schedule): Option[Int] = Some((sched.freq, sched.speed) match {
    case (Freq.Hourly, Speed.Bullet)     => 30
    case (Freq.Hourly, Speed.Bullitz)    => 45
    case (Freq.Hourly, Speed.Blitz)      => 50
    case (Freq.Hourly, Speed.Classical)  => 0 // N/A
    case (Freq.Daily, Speed.Bullet)      => 45
    case (Freq.Daily, Speed.Bullitz)     => 0 // N/A
    case (Freq.Daily, Speed.Blitz)       => 80
    case (Freq.Daily, Speed.Classical)   => 120
    case (Freq.Weekly, Speed.Bullet)     => 50
    case (Freq.Weekly, Speed.Bullitz)    => 0 // N/A
    case (Freq.Weekly, Speed.Blitz)      => 90
    case (Freq.Weekly, Speed.Classical)  => 120
    case (Freq.Monthly, Speed.Bullet)    => 60
    case (Freq.Monthly, Speed.Bullitz)   => 0 // N/A
    case (Freq.Monthly, Speed.Blitz)     => 100
    case (Freq.Monthly, Speed.Classical) => 150
  }) filter (0!=)

  private[tournament] def clockFor(sched: Schedule) = sched.speed match {
    case Schedule.Speed.Bullet    => TournamentClock(60, 0)
    case Schedule.Speed.Bullitz   => TournamentClock(3 * 60, 0)
    case Schedule.Speed.Blitz     => TournamentClock(5 * 60, 0)
    case Schedule.Speed.Classical => TournamentClock(10 * 60, 0)
  }
}
