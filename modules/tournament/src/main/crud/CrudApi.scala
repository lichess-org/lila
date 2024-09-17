package lila.tournament
package crud

import BSONHandlers._
import org.joda.time.DateTime

import lila.common.paginator.Paginator
import lila.db.dsl._
import lila.db.paginator.Adapter
import lila.user.User

final class CrudApi(tournamentRepo: TournamentRepo) {

  def list = tournamentRepo uniques 50

  def one(id: String) = tournamentRepo uniqueById id

  def editForm(tour: Tournament) =
    CrudForm.apply fill CrudForm.Data(
      name = tour.name,
      homepageHours = ~tour.spotlight.flatMap(_.homepageHours),
      timeControlSetup = TimeControl.DataForm.Setup(tour.timeControl),
      minutes = tour.minutes,
      variant = tour.variant.id,
      position = tour.position,
      date = tour.startsAt,
      image = ~tour.spotlight.flatMap(_.iconImg),
      headline = tour.spotlight.??(_.headline),
      description = tour.spotlight.??(_.description),
      conditions = Condition.DataForm.AllSetup(tour.conditions),
      berserkable = !tour.noBerserk,
      teamBattle = tour.isTeamBattle
    )

  def update(old: Tournament, data: CrudForm.Data) =
    tournamentRepo update updateTour(old, data) void

  def createForm = CrudForm.apply

  def create(data: CrudForm.Data, owner: User): Fu[Tournament] = {
    val tour = updateTour(empty, data).copy(createdBy = owner.id)
    tournamentRepo insert tour inject tour
  }

  def clone(old: Tournament) =
    old.copy(
      name = s"${old.name} (clone)",
      startsAt = DateTime.now plusDays 7
    )

  def paginator(page: Int)(implicit ec: scala.concurrent.ExecutionContext) =
    Paginator[Tournament](
      adapter = new Adapter[Tournament](
        collection = tournamentRepo.coll,
        selector = tournamentRepo.selectUnique,
        projection = none,
        sort = $doc("startsAt" -> -1)
      ),
      currentPage = page
    )

  private def empty =
    Tournament.make(
      by = Left(User.lishogiId),
      name = none,
      format = Format.Arena,
      timeControl = TimeControl.DataForm.Setup.default.convert,
      minutes = 0,
      variant = shogi.variant.Standard,
      position = none,
      mode = shogi.Mode.Rated,
      password = none,
      candidatesOnly = false,
      startDate = DateTime.now,
      berserkable = true,
      streakable = true,
      teamBattle = none,
      description = none,
      hasChat = true
    )

  private def updateTour(tour: Tournament, data: CrudForm.Data) = {
    import data._
    tour.copy(
      name = name,
      timeControl = timeControlSetup.convert,
      minutes = minutes,
      variant = realVariant,
      startsAt = date,
      schedule = Schedule(
        format = Format.Arena,
        freq = Schedule.Freq.Unique,
        speed = timeControlSetup.clock.map(Schedule.Speed.fromClock).getOrElse(Schedule.Speed.Correspondence),
        variant = realVariant,
        position = position,
        at = date
      ).some,
      spotlight = Spotlight(
        headline = headline,
        description = description,
        homepageHours = homepageHours.some.filterNot(0 ==),
        iconFont = none,
        iconImg = image.some.filter(_.nonEmpty)
      ).some,
      position = data.position,
      noBerserk = !data.berserkable,
      teamBattle = data.teamBattle option (tour.teamBattle | TeamBattle(Set.empty, 10))
    )
  }
}
