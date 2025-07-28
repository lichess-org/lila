package lila.tournament
package crud

import play.api.data.*
import play.api.data.Forms.*

import lila.common.Form.*

final class CrudForm(repo: TournamentRepo, forms: TournamentForm):

  import CrudForm.*

  def apply(tour: Option[Tournament])(using me: Me) = Form(
    mapping(
      "id" -> id[TourId](8, tour.map(_.id))(repo.exists),
      "homepageHours" -> number(min = 0, max = maxHomepageHours),
      "image" -> stringIn(imageChoices),
      "headline" -> text(minLength = 5, maxLength = 30),
      "teamBattle" -> boolean,
      "setup" -> forms.create(Nil).mapping
    )(Data.apply)(unapply)
  ).fill(
    Data(
      id = Tournament.makeId,
      homepageHours = 0,
      image = "",
      headline = "",
      teamBattle = false,
      setup = forms.empty()
    )
  )

  def edit(tour: Tournament)(using me: Me) = apply(tour.some).fill(
    Data(
      id = tour.id,
      homepageHours = ~tour.spotlight.flatMap(_.homepageHours),
      image = ~tour.spotlight.flatMap(_.iconImg),
      headline = tour.spotlight.so(_.headline),
      teamBattle = tour.isTeamBattle,
      setup = forms.fillFromTour(tour)
    )
  )

object CrudForm:

  val maxHomepageHours = 24

  val imageChoices = List(
    "" -> "Lichess",
    "offerspill.logo.png" -> "Offerspill"
  )
  val imageDefault = ""

  case class Data(
      id: TourId,
      homepageHours: Int,
      image: String,
      headline: String,
      teamBattle: Boolean,
      setup: TournamentSetup
  ):

    def toTour(using Me) =
      withCrud(
        Tournament
          .fromSetup(setup)
          .copy(
            id = id
          )
      )
    def update(old: Tournament) =
      withCrud(
        setup
          .updateAll(old)
      )

    private def withCrud(tour: Tournament) =
      tour.copy(
        schedule = Scheduled(freq = Schedule.Freq.Unique, at = tour.startsAt.dateTime).some,
        spotlight = Spotlight(
          headline = headline,
          homepageHours = homepageHours.some.filterNot(0 ==),
          iconFont = none,
          iconImg = image.some.filter(_.nonEmpty)
        ).some,
        teamBattle = teamBattle.option(tour.teamBattle | TeamBattle(Set.empty, 10))
      )
