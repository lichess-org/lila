package lila.streamer

import reactivemongo.api.*

import lila.common.paginator.{ AdapterLike, Paginator }
import lila.db.dsl.{ *, given }
import lila.user.{ Me, User, UserRepo }

final class StreamerPager(
    coll: Coll,
    userRepo: UserRepo,
    maxPerPage: lila.common.config.MaxPerPage,
    subsRepo: lila.relation.SubscriptionRepo
)(using Executor):

  import BsonHandlers.given

  def get(
      page: Int,
      live: LiveStreams,
      requests: Boolean
  )(using Option[Me.Id]): Fu[Paginator[Streamer.WithContext]] = Paginator(
    currentPage = page,
    maxPerPage = maxPerPage,
    adapter = if requests then approval else notLive(live)
  )

  def nextRequestId: Fu[Option[Streamer.Id]] = coll.primitiveOne[Streamer.Id](
    $doc("approval.requested" -> true, "approval.ignored" -> false),
    $sort asc "updatedAt",
    "_id"
  )

  private def notLive(live: LiveStreams)(using me: Option[Me.Id]): AdapterLike[Streamer.WithContext] = new:

    def nbResults: Fu[Int] = fuccess(1000)

    def slice(offset: Int, length: Int): Fu[Seq[Streamer.WithContext]] =
      coll
        .aggregateList(length, readPreference = ReadPreference.secondaryPreferred): framework =>
          import framework._
          Match(
            $doc(
              "approval.granted" -> true,
              "listed"           -> Streamer.Listed(true),
              "_id" $nin live.streams.map(_.streamer.id)
            )
          ) -> List(
            Sort(Descending("liveAt")),
            Skip(offset),
            Limit(3),
            PipelineOperator(userLookup),
            UnwindField("user")
          )
        .map: docs =>
          for
            doc      <- docs
            streamer <- doc.asOpt[Streamer]
            user     <- doc.getAsOpt[User]("user")
          yield Streamer.WithUser(streamer, user, false)
        .flatMap: streamers =>
          me.fold(fuccess(streamers)): me =>
            subsRepo.filterSubscribed(me, streamers.map(_.user.id)) map { subs =>
              streamers.map(s => s.copy(subscribed = subs(s.user.id)))
            }

  private def approval: AdapterLike[Streamer.WithContext] = new:

    private def selector = $doc("approval.requested" -> true, "approval.ignored" -> false)

    def nbResults: Fu[Int] = coll.countSel(selector)

    def slice(offset: Int, length: Int): Fu[Seq[Streamer.WithContext]] =
      coll
        .aggregateList(length, ReadPreference.secondaryPreferred): framework =>
          import framework._
          Match(selector) -> List(
            Sort(Ascending("updatedAt")),
            Skip(offset),
            Limit(length),
            PipelineOperator(userLookup),
            UnwindField("user")
          )
        .map: docs =>
          for
            doc      <- docs
            streamer <- doc.asOpt[Streamer]
            user     <- doc.getAsOpt[User]("user")
          yield Streamer.WithUser(streamer, user)

  private val userLookup = $lookup.simple(
    from = userRepo.coll,
    as = "user",
    local = "_id",
    foreign = "_id"
  )
