package lidraughts.streamer

import reactivemongo.api._

import lidraughts.common.paginator.Paginator
import lidraughts.db.dsl._
import lidraughts.db.paginator.{ Adapter, CachedAdapter }
import lidraughts.user.{ User, UserRepo }

final class StreamerPager(
    coll: Coll,
    maxPerPage: lidraughts.common.MaxPerPage
) {

  import BsonHandlers._

  def notLive(page: Int, live: LiveStreams, approvalRequested: Boolean = false): Fu[Paginator[Streamer.WithUser]] = {
    val adapter = new Adapter[Streamer](
      collection = coll,
      selector =
        if (approvalRequested) $doc(
          "approval.requested" -> true,
          "approval.ignored" -> false
        )
        else $doc(
          "approval.granted" -> true,
          "listed" -> Streamer.Listed(true),
          "_id" $nin live.streams.map(_.streamer.id)
        ),
      projection = $empty,
      sort = $doc("liveAt" -> -1)
    ) mapFutureList withUsers
    Paginator(
      adapter = new CachedAdapter(adapter, nbResults = fuccess(6000)),
      currentPage = page,
      maxPerPage = maxPerPage
    )
  }

  private def withUsers(streamers: Seq[Streamer]): Fu[Seq[Streamer.WithUser]] =
    UserRepo.withColl {
      _.optionsByOrderedIds[User, User.ID](streamers.map(_.id.value), ReadPreference.secondaryPreferred)(_.id)
    } map { users =>
      streamers zip users collect {
        case (streamer, Some(user)) => Streamer.WithUser(streamer, user)
      }
    }
}
