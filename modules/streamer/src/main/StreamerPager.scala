package lila.streamer

import reactivemongo.api.*

import lila.common.paginator.{ AdapterLike, Paginator }
import lila.db.dsl.{ *, given }
import lila.user.{ User, UserRepo }

final class StreamerPager(
    coll: Coll,
    userRepo: UserRepo,
    maxPerPage: lila.common.config.MaxPerPage,
    subsRepo: lila.relation.SubscriptionRepo
)(using ec: scala.concurrent.ExecutionContext):

  import BsonHandlers.given

  def get(
      page: Int,
      live: LiveStreams,
      forUser: Option[UserId],
      requests: Boolean
  ): Fu[Paginator[Streamer.WithContext]] = Paginator(
    currentPage = page,
    maxPerPage = maxPerPage,
    adapter = if (requests) approval else notLive(live, forUser)
  )

  def nextRequestId: Fu[Option[Streamer.Id]] = coll.primitiveOne[Streamer.Id](
    $doc("approval.requested" -> true, "approval.ignored" -> false),
    $sort asc "updatedAt",
    "_id"
  )

  private def notLive(live: LiveStreams, forUser: Option[UserId]): AdapterLike[Streamer.WithContext] =
    new AdapterLike[Streamer.WithContext]:

      def nbResults: Fu[Int] = fuccess(1000)

      def slice(offset: Int, length: Int): Fu[Seq[Streamer.WithContext]] =
        coll
          .aggregateList(length, readPreference = ReadPreference.secondaryPreferred) { framework =>
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
              Limit(length),
              PipelineOperator(
                $lookup.simple(
                  from = userRepo.coll,
                  as = "user",
                  local = "_id",
                  foreign = "_id"
                )
              ),
              UnwindField("user"),
              PipelineOperator(
                $lookup.simple(
                  from = subsRepo.coll,
                  as = "subs",
                  local = "_id",
                  foreign = "s"
                )
              ),
              AddFields($doc("subscribed" -> $doc("$in" -> List(forUser.??(_.toString), "$subs.u"))))
            )
          }
          .map { docs =>
            for {
              doc        <- docs
              streamer   <- doc.asOpt[Streamer]
              user       <- doc.getAsOpt[User]("user")
              subscribed <- doc.getAsOpt[Boolean]("subscribed")
            } yield Streamer.WithUser(streamer, user, subscribed)
          }

  private def approval: AdapterLike[Streamer.WithContext] = new AdapterLike[Streamer.WithContext]:

    private def selector = $doc("approval.requested" -> true, "approval.ignored" -> false)

    def nbResults: Fu[Int] = coll.countSel(selector)

    def slice(offset: Int, length: Int): Fu[Seq[Streamer.WithContext]] =
      coll
        .aggregateList(length, ReadPreference.secondaryPreferred) { framework =>
          import framework._
          Match(selector) -> List(
            Sort(Ascending("updatedAt")),
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
