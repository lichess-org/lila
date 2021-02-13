package lila.streamer

import reactivemongo.api._

import lila.common.paginator.Paginator
import lila.db.dsl._
import lila.db.paginator.{ Adapter, CachedAdapter }
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
    val adapter = new Adapter[Streamer](
      collection = coll,
      selector =
        if (approvalRequested) approvalRequestedSelector
        else
          $doc(
            "approval.granted" -> true,
            "listed"           -> Streamer.Listed(true),
            "_id" $nin live.streams.map(_.streamer.id)
          ),
      projection = none,
      sort = if (approvalRequested) $sort asc "updatedAt" else $sort desc "liveAt"
    ) mapFutureList withUsers
    Paginator(
      adapter = new CachedAdapter(adapter, nbResults = fuccess(6000)),
      currentPage = page,
      maxPerPage = maxPerPage
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

  private def withUsers(streamers: Seq[Streamer]): Fu[Seq[Streamer.WithUser]] =
    userRepo.withColl {
      _.optionsByOrderedIds[User, User.ID](
        streamers.map(_.id.value),
        none,
        ReadPreference.secondaryPreferred
      )(_.id)
    } map { users =>
      streamers zip users collect { case (streamer, Some(user)) =>
        Streamer.WithUser(streamer, user)
      }
    }
}
