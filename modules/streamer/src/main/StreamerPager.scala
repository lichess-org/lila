package lila.streamer

import reactivemongo.api._

import lila.common.paginator.{ AdapterLike, Paginator }
import lila.db.dsl._
import lila.user.{ User, UserRepo }

final class StreamerPager(
    coll: Coll,
    userRepo: UserRepo,
    maxPerPage: lila.common.config.MaxPerPage
)(implicit ec: scala.concurrent.ExecutionContext) {

  import BsonHandlers._

  def notLive(
      page: Int,
      live: LiveStreams,
      approvalRequested: Boolean = false
  ): Fu[Paginator[Streamer.WithUser]] = {
    Paginator(
      currentPage = page,
      maxPerPage = maxPerPage,
      adapter = new AdapterLike[Streamer.WithUser] {

        private def selector =
          if (approvalRequested) approvalRequestedSelector
          else
            $doc(
              "approval.granted" -> true,
              "listed"           -> Streamer.Listed(true),
              "_id" $nin live.streams.map(_.streamer.id)
            )

        def nbResults: Fu[Int] =
          if (approvalRequested) coll.countSel(selector)
          else fuccess(1000)

        def slice(offset: Int, length: Int): Fu[Seq[Streamer.WithUser]] =
          coll
            .aggregateList(length, readPreference = ReadPreference.secondaryPreferred) { framework =>
              import framework._
              Match(selector) -> List(
                Sort(if (approvalRequested) Ascending("updatedAt") else Descending("liveAt")),
                Skip(offset),
                Limit(length),
                PipelineOperator(
                  $lookup.simple(
                    from = userRepo.coll,
                    as = "user",
                    local = "_id",
                    foreign = "_id"
                  )
                ),
                UnwindField("user")
              )
            }
            .map { docs =>
              for {
                doc      <- docs
                streamer <- doc.asOpt[Streamer]
                user     <- doc.getAsOpt[User]("user")
              } yield Streamer.WithUser(streamer, user)
            }
      }
    )
  }

  def nextRequestId: Fu[Option[Streamer.Id]] =
    coll.primitiveOne[Streamer.Id](
      $doc(approvalRequestedSelector),
      $sort asc "updatedAt",
      "_id"
    )

  private val approvalRequestedSelector =
    $doc(
      "approval.requested" -> true,
      "approval.ignored"   -> false
    )
}
