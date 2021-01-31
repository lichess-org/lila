package lila.tournament
package crud

import BSONHandlers._
import org.joda.time.DateTime
import scala.util.chaining._

import lila.common.config.MaxPerPage
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
      clockTime = tour.clock.limitInMinutes,
      clockIncrement = tour.clock.incrementSeconds,
      minutes = tour.minutes,
      variant = tour.variant.id,
      position = tour.position,
      date = tour.startsAt,
      image = ~tour.spotlight.flatMap(_.iconImg),
      headline = tour.spotlight.??(_.headline),
      description = tour.spotlight.??(_.description),
      conditions = Condition.DataForm.AllSetup(tour.conditions),
      berserkable = !tour.noBerserk,
      streakable = tour.streakable,
      teamBattle = tour.isTeamBattle,
      hasChat = tour.hasChat
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
      currentPage = page,
      maxPerPage = MaxPerPage(20)
    )

  private def empty =
    Tournament.make(
      by = Left(User.lichessId),
      name = none,
      clock = chess.Clock.Config(0, 0),
      minutes = 0,
      variant = chess.variant.Standard,
      position = none,
      mode = chess.Mode.Rated,
      password = None,
      waitMinutes = 0,
      startDate = none,
      berserkable = true,
      streakable = true,
      teamBattle = none,
      description = none,
      hasChat = true
    )

  private def updateTour(tour: Tournament, data: CrudForm.Data) = {
    import data._
    val clock = chess.Clock.Config((clockTime * 60).toInt, clockIncrement)
    tour.copy(
      name = name,
      clock = clock,
      minutes = minutes,
      variant = realVariant,
      startsAt = date,
      schedule = Schedule(
        freq = Schedule.Freq.Unique,
        speed = Schedule.Speed.fromClock(clock),
        variant = realVariant,
        position = realPosition,
        at = date
      ).some,
      spotlight = Spotlight(
        headline = headline,
        description = description,
        homepageHours = homepageHours.some.filterNot(0 ==),
        iconFont = none,
        iconImg = image.some.filter(_.nonEmpty)
      ).some,
      position = data.realPosition,
      noBerserk = !data.berserkable,
      noStreak = !data.streakable,
      teamBattle = data.teamBattle option (tour.teamBattle | TeamBattle(Set.empty, 10)),
      hasChat = data.hasChat
    ) pipe { tour =>
      tour.copy(conditions =
        data.conditions.convert(tour.perfType, Map.empty)
      ) // the CRUD form doesn't support team restrictions so Map.empty is fine
    }
  }
}
