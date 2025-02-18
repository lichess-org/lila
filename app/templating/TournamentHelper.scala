package lila.app
package templating

import controllers.routes
import play.api.i18n.Lang

import lila.app.ui.ScalatagsTemplate._
import lila.i18n.{ I18nKeys => trans }
import lila.tournament.Schedule
import lila.tournament.Tournament

trait TournamentHelper { self: I18nHelper with DateHelper with UserHelper =>

  def tournamentIdToName(id: String)(implicit lang: Lang) =
    env.tournament.getTourName get id getOrElse trans.tournament.txt()

  def tournamentLink(tourId: String)(implicit lang: Lang): Frag =
    a(
      dataIcon := "g",
      cls      := "text",
      href     := routes.Tournament.show(tourId).url,
    )(tournamentIdToName(tourId))

  def tournamentLink(tour: Tournament)(implicit lang: Lang): Frag =
    a(
      dataIcon := "g",
      cls      := (if (tour.isScheduled) "text is-gold" else "text"),
      href     := routes.Tournament.show(tour.id).url,
    )(tour.trans)

  def tournamentIconChar(tour: Tournament): String =
    tour.schedule.map(_.freq) match {
      case Some(Schedule.Freq.Marathon) => "\\"
      case Some(Schedule.Freq.Unique)   => "☗"
      case _ => tour.spotlight.flatMap(_.iconFont) | tour.perfType.iconChar.toString
    }
}
