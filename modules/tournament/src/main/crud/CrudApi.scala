package lila.tournament
package crud

import chess.Mode
import scalalib.paginator.Paginator

import lila.db.dsl.*
import lila.db.paginator.Adapter
import lila.tournament.BSONHandlers.given

final class CrudApi(tournamentRepo: TournamentRepo, tourApi: TournamentApi, crudForm: CrudForm):

  def list = tournamentRepo.uniques(50)

  export tournamentRepo.{ uniqueById as one }

  def editForm(tour: Tournament)(using Me) =
    crudForm.edit(tour)

  def update(old: Tournament, data: CrudForm.Data) =
    tourApi.updateTour(old, data.setup, data.update(old)).void

  def createForm(using Me) = crudForm(none)

  def create(data: CrudForm.Data)(using Me): Fu[Tournament] =
    val tour = data.toTour
    tournamentRepo.insert(tour).inject(tour)

  def clone(old: Tournament) =
    old.copy(
      name = s"${old.name} (clone)",
      startsAt = nowInstant.plusDays(7)
    )

  def paginator(page: Int)(using Executor) =
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
