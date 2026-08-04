package lila.streamer

import reactivemongo.api.*
import scalalib.paginator.{ AdapterLike, Paginator }
import scalalib.model.Language

import lila.db.dsl.{ *, given }

final class StreamerPager(
    coll: Coll,
    userRepo: lila.core.user.UserRepo,
    maxPerPage: MaxPerPage,
    subsRepo: lila.core.relation.SubscriptionRepo
)(using Executor):

  import BsonHandlers.given

  def get(
      page: Int,
      live: LiveStreams,
      requests: Boolean,
      lang: Option[Language]
  )(using Option[MyId]): Fu[Paginator[Streamer.WithContext]] = Paginator(
    currentPage = page,
    maxPerPage = maxPerPage,
    adapter = if requests then approval else notLive(live, lang)
  )

  def nextRequestId: Fu[Option[Streamer.Id]] = coll.primitiveOne[Streamer.Id](
    $doc("approval.requested" -> true, "approval.ignored" -> false),
    $sort.asc("updatedAt"),
    "_id"
  )

  private def notLive(live: LiveStreams, lang: Option[Language])(using
      me: Option[MyId]
  ): AdapterLike[Streamer.WithContext] = new:

    def nbResults: Fu[Int] = fuccess(1000)

    def slice(offset: Int, length: Int): Fu[Seq[Streamer.WithContext]] =
      coll
        .aggregateList(length, _.sec): framework =>
          import framework.*
          Match(
            $doc(
              "approval.granted" -> true,
              "listed" -> Streamer.Listed(true),
              "_id".$nin(live.streams.map(_.streamer.id))
            ) ++ lang.fold($doc())(l => $doc("lastStreamLang" -> l))
          ) -> List(
            Sort(Descending("liveAt")),
            Skip(offset),
            Limit(
              length
            ), /*  was hardcoded to 3, causing Skip's offset (based on maxPerPage) to overshoot and silently drop streamers between pages, like 20
             */
            PipelineOperator(userLookup),
            UnwindField("user")
          )
        .map: docs =>
          import userRepo.userHandler
          for
            doc <- docs
            streamer <- doc.asOpt[Streamer]
            user <- doc.getAsOpt[User]("user")
          yield Streamer.WithUser(streamer, user, false)
        .flatMap: streamers =>
          me.fold(fuccess(streamers)): me =>
            subsRepo.filterSubscribed(me, streamers.map(_.user.id)).map { subs =>
              streamers.map(s => s.copy(subscribed = subs(s.user.id)))
            }

  private def approval: AdapterLike[Streamer.WithContext] = new:

    private def selector = $doc("approval.requested" -> true, "approval.ignored" -> false)

    def nbResults: Fu[Int] = coll.countSel(selector)

    def slice(offset: Int, length: Int): Fu[Seq[Streamer.WithContext]] =
      coll
        .aggregateList(length, _.sec): framework =>
          import framework.*
          Match(selector) -> List(
            Sort(Ascending("updatedAt")),
            Skip(offset),
            Limit(length),
            PipelineOperator(userLookup),
            UnwindField("user")
          )
        .map: docs =>
          import userRepo.userHandler
          for
            doc <- docs
            streamer <- doc.asOpt[Streamer]
            user <- doc.getAsOpt[User]("user")
          yield Streamer.WithUser(streamer, user)

  private val userLookup = $lookup.simple(
    from = userRepo.coll,
    as = "user",
    local = "_id",
    foreign = "_id"
  )
