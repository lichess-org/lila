package lidraughts.tournament
package crud

import lidraughts.user.User
import lidraughts.user.UserRepo.lidraughtsId

final class CrudApi {

  def list = TournamentRepo uniques 50

  def one(id: String) = TournamentRepo uniqueById id

  def editForm(tour: Tournament) = CrudForm.apply fill CrudForm.Data(
    name = tour.name,
    homepageHours = ~tour.spotlight.flatMap(_.homepageHours),
    clockTime = tour.clock.limitInMinutes,
    clockIncrement = tour.clock.incrementSeconds,
    minutes = tour.minutes,
    variant = tour.variant.id,
    position = tour.position.fen,
    date = tour.startsAt,
    image = ~tour.spotlight.flatMap(_.iconImg),
    headline = tour.spotlight.??(_.headline),
    description = tour.spotlight.??(_.description),
    conditions = Condition.DataForm.AllSetup(tour.conditions)
  )

  def update(old: Tournament, data: CrudForm.Data) =
    TournamentRepo update updateTour(old, data) void

  def createForm = CrudForm.apply

  def create(data: CrudForm.Data, owner: User): Fu[Tournament] = {
    val tour = updateTour(empty, data).copy(createdBy = owner.id)
    TournamentRepo insert tour inject tour
  }

  private def empty = Tournament.make(
    by = Left(lidraughtsId),
    name = none,
    clock = draughts.Clock.Config(0, 0),
    minutes = 0,
    system = System.Arena,
    variant = draughts.variant.Standard,
    position = draughts.StartingPosition.initial,
    mode = draughts.Mode.Rated,
    `private` = false,
    password = None,
    waitMinutes = 0
  )

  private def updateTour(tour: Tournament, data: CrudForm.Data) = {
    import data._
    val clock = draughts.Clock.Config((clockTime * 60).toInt, clockIncrement)
    val v = draughts.variant.Variant.orDefault(variant)
    tour.copy(
      name = name,
      clock = clock,
      minutes = minutes,
      variant = v,
      startsAt = date,
      schedule = Schedule(
        freq = Schedule.Freq.Unique,
        speed = Schedule.Speed.fromClock(clock),
        variant = v,
        position = draughts.StartingPosition.initial,
        at = date
      ).some,
      spotlight = Spotlight(
        headline = headline,
        description = description,
        homepageHours = homepageHours.some.filterNot(0 ==),
        iconFont = none,
        iconImg = image.some.filter(_.nonEmpty)
      ).some,
      position = DataForm.startingPosition(data.position, v),
      conditions = data.conditions.convert
    )
  }
}
