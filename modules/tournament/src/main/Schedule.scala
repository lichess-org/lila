package lila.tournament

import org.joda.time.DateTime

case class Schedule(
    freq: Schedule.Freq,
    speed: Schedule.Speed,
    at: DateTime) {

  def name = s"Lichess ${freq.toString} ${speed.toString}"

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
    case object Blitz extends Speed
    case object Slow extends Speed
    val all: List[Speed] = List(Bullet, Blitz, Slow)
    def apply(name: String) = all find (_.name == name)
  }

  private[tournament] def durationFor(sched: Schedule): Option[Int] = Some((sched.freq, sched.speed) match {
    case (Freq.Hourly, Speed.Bullet)  => 30
    case (Freq.Hourly, Speed.Blitz)   => 50
    case (Freq.Hourly, Speed.Slow)    => 0 // N/A
    case (Freq.Daily, Speed.Bullet)   => 45
    case (Freq.Daily, Speed.Blitz)    => 80
    case (Freq.Daily, Speed.Slow)     => 120
    case (Freq.Weekly, Speed.Bullet)  => 50
    case (Freq.Weekly, Speed.Blitz)   => 90
    case (Freq.Weekly, Speed.Slow)    => 120
    case (Freq.Monthly, Speed.Bullet) => 60
    case (Freq.Monthly, Speed.Blitz)  => 100
    case (Freq.Monthly, Speed.Slow)   => 150
  }) filter (0!=)

  private[tournament] def clockFor(sched: Schedule) = sched.speed match {
    case Schedule.Speed.Bullet => TournamentClock(60, 0)
    case Schedule.Speed.Blitz  => TournamentClock(5 * 60, 0)
    case Schedule.Speed.Slow   => TournamentClock(10 * 60, 0)
  }

  import lila.db.JsTube
  import JsTube.Helpers._
  import play.api.libs.json._
  private implicit val freqFormat = Format[Freq](
    Reads[Freq] {
      case JsString(name) => Freq(name) match {
        case Some(freq) => JsSuccess(freq)
        case None       => JsError()
      }
      case _ => JsError()
    },
    Writes[Freq](freq => JsString(freq.name)))
  private implicit val speedFormat = Format[Speed](
    Reads[Speed] {
      case JsString(name) => Speed(name) match {
        case Some(speed) => JsSuccess(speed)
        case None        => JsError()
      }
      case _ => JsError()
    },
    Writes[Speed](speed => JsString(speed.name)))
  private[tournament] val tube = JsTube(
    (__.json update readDate('at)) andThen Json.reads[Schedule],
    Json.writes[Schedule] andThen (__.json update  writeDate('at)))
}
