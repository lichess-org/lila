package lila.push

import org.apache.pekko.stream.scaladsl.Source
import reactivemongo.api.bson.*
import reactivemongo.pekkostream.cursorProducer

import lila.core.id.SessionId
import lila.core.misc.oauth.AccessTokenId
import lila.db.dsl.{ *, given }

final class WebSubscriptionApi(coll: Coll)(using Executor):

  import WebSubscription.given

  def subscribe(user: User, subscription: WebSubscription, id: SessionId | AccessTokenId): Funit =
    coll.update
      .one(
        bid(id.toString),
        bdoc(
          "userId" -> user.id,
          "endpoint" -> subscription.endpoint,
          "auth" -> subscription.auth,
          "p256dh" -> subscription.p256dh,
          "seenAt" -> nowInstant
        ),
        upsert = true
      )
      .void

  def unsubscribeBySession(id: SessionId | AccessTokenId): Funit =
    coll.delete.one(bid(id.toString)).void

  def unsubscribeByUser(user: User): Funit =
    coll.delete.one(bdoc("userId" -> user.id)).void

  // userIds is necessary to match the mongodb index
  private[push] def unsubscribeByEndpoints(endpoints: Iterable[String], userIds: Iterable[UserId]): Fu[Int] =
    endpoints.nonEmpty.so:
      coll.delete.one(bdoc("userId".in(userIds), "endpoint".in(endpoints))).map(_.n)

  private[push] def getSubscriptions(max: Int)(userId: UserId): Fu[List[WebSubscription]] =
    coll
      .find(bdoc("userId" -> userId), bdoc("endpoint" -> true, "auth" -> true, "p256dh" -> true).some)
      .sort(bdoc("seenAt" -> -1))
      .cursor[WebSubscription](ReadPref.sec)
      .list(max)

  private[push] def getSubscriptions(
      allUserIds: Iterable[UserId],
      maxPerUser: Int
  ): Source[(List[WebSubscription], Iterable[UserId]), ?] =
    val groupSize = 50
    Source(allUserIds.grouped(groupSize).toList)
      .throttle(10, 1.second)
      .mapAsync(1): group =>
        coll
          .aggregateWith[WebSubscription](readPreference = ReadPref.sec): framework =>
            import framework.*
            List(
              Match("userId".in(group)),
              Sort(Descending("seenAt")),
              GroupField("userId")("subs" -> Push(BSONString("$$ROOT"))),
              Project(bdoc("subs" -> Slice(BSONString("$subs"), BSONInteger(maxPerUser)), "_id" -> false)),
              Unwind("subs"),
              ReplaceRootField("subs")
            )
          .collect[List](groupSize * maxPerUser)
          .map(_ -> group)
