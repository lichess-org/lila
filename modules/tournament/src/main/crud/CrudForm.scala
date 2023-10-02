package lila.tournament
package crud

import play.api.data.*
import play.api.data.Forms.*

import chess.variant.Variant
import chess.format.Fen
import chess.Clock.IncrementSeconds
import lila.common.Form.{ given, * }
import lila.user.Me
import lila.gathering.GatheringClock

final class CrudForm(repo: TournamentRepo, forms: TournamentForm):

  import CrudForm.*
  import TournamentForm.*

  def apply(tour: Option[Tournament])(using me: Me) = Form(
    mapping(
      "id"            -> id[TourId](8, tour.map(_.id))(repo.exists),
      "homepageHours" -> number(min = 0, max = maxHomepageHours),
      "image"         -> stringIn(imageChoices),
      "headline"      -> text(minLength = 5, maxLength = 30),
      "setup"         -> forms.create(Nil).mapping
    )(Data.apply)(unapply)
  ) fill Data(
    id = Tournament.makeId,
    homepageHours = 0,
    image = "",
    headline = "",
    setup = forms.empty()
  )

  def edit(tour: Tournament)(using me: Me) = apply(tour.some) fill
    Data(
      id = tour.id,
      homepageHours = ~tour.spotlight.flatMap(_.homepageHours),
      image = ~tour.spotlight.flatMap(_.iconImg),
      headline = tour.spotlight.so(_.headline),
      setup = forms.fillFromTour(tour)
    )

object CrudForm:

  val maxHomepageHours = 24

  val imageChoices = List(
    ""                    -> "Lichess",
    "offerspill.logo.png" -> "Offerspill"
  )
  val imageDefault = ""

  case class Data(
      id: TourId,
      homepageHours: Int,
      image: String,
      headline: String,
      setup: TournamentSetup
  ):

    def toTour(using Me) =
      withSchedule(
        Tournament
          .fromSetup(setup)
          .copy(
            id = id,
            spotlight = Spotlight(
              headline = headline,
              homepageHours = homepageHours.some.filterNot(0 ==),
              iconFont = none,
              iconImg = image.some.filter(_.nonEmpty)
            ).some
          )
      )
    def update(old: Tournament) =
      withSchedule(
        setup
          .updateAll(old)
          .copy(spotlight =
            Spotlight(
              headline = headline,
              homepageHours = homepageHours.some.filterNot(0 ==),
              iconFont = none,
              iconImg = image.some.filter(_.nonEmpty)
            ).some
          )
      )

    private def withSchedule(tour: Tournament) =
      tour.copy(
        schedule = Schedule(
          freq = Schedule.Freq.Unique,
          speed = Schedule.Speed.fromClock(tour.clock),
          variant = tour.variant,
          position = tour.position,
          at = tour.startsAt.dateTime
        ).some
      )
