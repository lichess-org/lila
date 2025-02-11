package lila.app
package templating

import play.api.i18n.Lang
import play.api.libs.json.Json

import lila.app.ui.ScalatagsTemplate._
import lila.i18n.{ I18nKeys => trans }
import lila.tournament.Format
import lila.tournament.Schedule
import lila.tournament.Tournament
import lila.tournament.TournamentShield
import lila.tournament.Winner
import lila.user.User

trait TournamentHelper { self: I18nHelper with DateHelper with UserHelper with ShogiHelper =>

  def netBaseUrl: String

  def tournamentJsData(tour: Tournament, version: Int, user: Option[User]) = {

    val data = Json.obj(
      "tournament" -> Json.obj("id" -> tour.id),
      "version"    -> version,
    )
    Json stringify {
      user.fold(data) { u =>
        data ++ Json.obj("username" -> u.username)
      }
    }
  }

  def idToTournamentName(id: Tournament.ID)(implicit lang: Lang): Fu[Option[String]] =
    env.tournament.api.get(id) dmap2 tournamentName

  def tournamentName(tour: Tournament)(implicit lang: Lang): String =
    if (tour.isMarathon || tour.isUnique) tour.name
    else if (tour.isTeamBattle) trans.tourname.xTeamBattle.txt(tour.name)
    else tour.schedule.fold(tour.name)(scheduledTournamentName)

  def shieldName(cat: TournamentShield.Category)(implicit lang: Lang): String =
    cat.of.fold(s => speedName(s), v => variantName(v))

  def winnerTournamentName(w: Winner)(implicit lang: Lang): String =
    w.schedule.flatMap(Schedule.fromNameKeys).fold(w.tourName)(scheduledTournamentName)

  private def scheduledTournamentName(s: Schedule)(implicit lang: Lang): String = {
    val sched =
      s"${freqName(s.freq, if (s.variant.standard) speedName(s.speed) else variantName(s.variant))}"
    if (s.format == Format.Arena) trans.tourname.xArena.txt(sched)
    else if (s.format == Format.Robin) trans.tourname.xRobin.txt(sched)
    else sched
  }

  private def freqName(freq: Schedule.Freq, x: String)(implicit lang: Lang): String =
    freq match {
      case Schedule.Freq.Hourly   => trans.tourname.hourlyX.txt(x)
      case Schedule.Freq.Daily    => trans.tourname.dailyX.txt(x)
      case Schedule.Freq.Eastern  => trans.tourname.easternX.txt(x)
      case Schedule.Freq.Weekly   => trans.tourname.weeklyX.txt(x)
      case Schedule.Freq.Weekend  => trans.tourname.weekendX.txt(x)
      case Schedule.Freq.Monthly  => trans.tourname.monthlyX.txt(x)
      case Schedule.Freq.Shield   => trans.tourname.xShield.txt(x)
      case Schedule.Freq.Marathon => "Marathon"
      case Schedule.Freq.Yearly   => trans.tourname.yearlyX.txt(x)
      case Schedule.Freq.Unique   => s"${name} $x"
    }

  private def speedName(speed: Schedule.Speed)(implicit lang: Lang): String = {
    val specialPrefix =
      speed match {
        case Schedule.Speed.UltraBullet                             => "U-"
        case Schedule.Speed.HyperBullet | Schedule.Speed.HyperRapid => "H-"
        case Schedule.Speed.SuperBlitz                              => "S-"
        case _                                                      => ""
      }
    s"${specialPrefix}${Schedule.Speed.toPerfType(speed).trans}"
  }

  def tournamentIconChar(tour: Tournament): String =
    tour.schedule.map(_.freq) match {
      case Some(Schedule.Freq.Marathon) => "\\"
      case Some(Schedule.Freq.Unique)   => "☗"
      case _ => tour.spotlight.flatMap(_.iconFont) | tour.perfType.iconChar.toString
    }
}
