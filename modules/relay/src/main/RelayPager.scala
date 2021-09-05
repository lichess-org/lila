package lila.relay

import reactivemongo.api.ReadPreference

import lila.common.config.MaxPerPage
import lila.common.paginator.{ AdapterLike, Paginator }
import lila.db.dsl._

final class RelayPager(tourRepo: RelayTourRepo, roundRepo: RelayRoundRepo)(implicit
    ec: scala.concurrent.ExecutionContext
) {

  import BSONHandlers._

  def inactive(page: Int): Fu[Paginator[RelayTour.WithLastRound]] =
    Paginator(
      adapter = new AdapterLike[RelayTour.WithLastRound] {

        def nbResults: Fu[Int] = fuccess(9999)

        def slice(offset: Int, length: Int): Fu[List[RelayTour.WithLastRound]] =
          tourRepo.coll
            .aggregateList(length, readPreference = ReadPreference.secondaryPreferred) { framework =>
              import framework._
              Match(tourRepo.selectors.official ++ tourRepo.selectors.inactive) -> List(
                Sort(Descending("syncedAt")),
                PipelineOperator(
                  $lookup.pipeline(
                    from = roundRepo.coll,
                    as = "round",
                    local = "_id",
                    foreign = "tourId",
                    pipeline = List(
                      $doc(
                        "$sort" -> $doc(
                          "startedAt" -> -1,
                          "startsAt"  -> -1,
                          "name"      -> -1
                        )
                      ),
                      $doc("$limit"     -> 1),
                      $doc("$addFields" -> $doc("sync.log" -> $arr()))
                    )
                  )
                ),
                UnwindField("round")
              )
            }
            .map { docs =>
              for {
                doc   <- docs
                tour  <- doc.asOpt[RelayTour]
                round <- doc.getAsOpt[RelayRound]("round")
              } yield RelayTour.WithLastRound(tour, round)
            }
      },
      currentPage = page,
      maxPerPage = MaxPerPage(20)
    )
}
