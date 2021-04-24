package lila.relay

import reactivemongo.api.ReadPreference

import lila.common.config.MaxPerPage
import lila.common.paginator.{ AdapterLike, Paginator }
import lila.db.dsl._
import lila.study.StudyApi
import lila.user.User

final class RelayPager(roundRepo: RelayRoundRepo, tourRepo: RelayTourRepo, studyApi: StudyApi)(implicit
    ec: scala.concurrent.ExecutionContext
) {

  import BSONHandlers._

  private lazy val maxPerPage = MaxPerPage(20)

  def finished(me: Option[User], page: Int) =
    paginator(
      roundRepo.selectors finished true,
      me,
      page,
      fuccess(9999).some
    )

  private def paginator(
      selector: Bdoc,
      me: Option[User],
      page: Int,
      nbResults: Option[Fu[Int]]
  ): Fu[Paginator[RelayRound.WithTour]] =
    Paginator(
      adapter = new AdapterLike[RelayRound.WithTour] {

        def nbResults: Fu[Int] = roundRepo.coll.secondaryPreferred.countSel(selector)

        def slice(offset: Int, length: Int): Fu[List[RelayRound.WithTour]] =
          roundRepo.coll
            .aggregateList(length, readPreference = ReadPreference.secondaryPreferred) { framework =>
              import framework._
              Sort(Descending("startedAt")) -> List(
                GroupField("tourId")("round" -> FirstField("$ROOT")),
                Skip(offset),
                Limit(length),
                ReplaceRootField("round"),
                AddFields($doc("sync.log" -> $arr())),
                PipelineOperator(tourRepo lookup "tourId"),
                UnwindField("tour")
              )
            }
            .map(_ flatMap readRelayWithTour)
      },
      // }.mapFutureList(withStudy),
      currentPage = page,
      maxPerPage = maxPerPage
    )

  private def withStudy(rts: Seq[RelayRound.WithTour]): Fu[Seq[RelayRound.WithTourAndStudy]] =
    studyApi byIds rts.map(_.relay.studyId) map { studies =>
      rts.flatMap { rt =>
        studies.find(_.id == rt.relay.studyId) map rt.withStudy
      }
    }
}
