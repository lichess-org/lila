package lila.relay

import reactivemongo.api.ReadPreference

import lila.common.config.MaxPerPage
import lila.common.paginator.{ AdapterLike, Paginator }
import lila.db.dsl._
import lila.study.StudyApi
import lila.user.User

final class RelayPager(relayRepo: RelayRepo, tourRepo: RelayTourRepo, studyApi: StudyApi)(implicit
    ec: scala.concurrent.ExecutionContext
) {

  import BSONHandlers._

  private lazy val maxPerPage = MaxPerPage(20)

  def finished(me: Option[User], page: Int) =
    paginator(
      relayRepo.selectors finished true,
      me,
      page,
      fuccess(9999).some
    )

  private def paginator(
      selector: Bdoc,
      me: Option[User],
      page: Int,
      nbResults: Option[Fu[Int]]
  ): Fu[Paginator[Relay.WithTour]] =
    Paginator(
      adapter = new AdapterLike[Relay.WithTour] {

        def nbResults: Fu[Int] = relayRepo.coll.secondaryPreferred.countSel(selector)

        def slice(offset: Int, length: Int): Fu[List[Relay.WithTour]] =
          relayRepo.coll
            .aggregateList(length, readPreference = ReadPreference.secondaryPreferred) { framework =>
              import framework._
              Match(selector) -> List(
                Sort(Descending("startedAt")),
                Skip(offset),
                Limit(length),
                PipelineOperator(tourRepo lookup "tourId"),
                Unwind("tour")
              )
            }
            .map(_ flatMap readRelayWithTour)
      },
      // }.mapFutureList(withStudy),
      currentPage = page,
      maxPerPage = maxPerPage
    )

  private def withStudy(rts: Seq[Relay.WithTour]): Fu[Seq[Relay.WithTourAndStudy]] =
    studyApi byIds rts.map(_.relay.studyId) map { studies =>
      rts.flatMap { rt =>
        studies.find(_.id == rt.relay.studyId) map rt.withStudy
      }
    }
}
