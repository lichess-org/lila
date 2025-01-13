package lila.push

import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }

final class WebSubscriptionApi(coll: Coll)(using Executor):

  import WebSubscription.given

  def subscribe(user: User, subscription: WebSubscription, sessionId: String): Funit =
    coll.update
      .one(
        $id(sessionId),
        $doc(
          "userId"   -> user.id,
          "endpoint" -> subscription.endpoint,
          "auth"     -> subscription.auth,
          "p256dh"   -> subscription.p256dh,
          "seenAt"   -> nowInstant
        ),
        upsert = true
      )
      .void

  def unsubscribeBySession(sessionId: String): Funit =
    coll.delete.one($id(sessionId)).void

  def unsubscribeByUser(user: User): Funit =
    coll.delete.one($doc("userId" -> user.id)).void

  // userIds is necessary to match the mongodb index
  def unsubscribeByEndpoints(endpoints: Iterable[String], userIds: Iterable[UserId]): Fu[Int] =
    endpoints.nonEmpty.so:
      coll.delete.one($doc("userId".$in(userIds), "endpoint".$in(endpoints))).map(_.n)

  private[push] def getSubscriptions(max: Int)(userId: UserId): Fu[List[WebSubscription]] =
    coll
      .find($doc("userId" -> userId), $doc("endpoint" -> true, "auth" -> true, "p256dh" -> true).some)
      .sort($doc("seenAt" -> -1))
      .cursor[WebSubscription](ReadPref.sec)
      .list(max)

  private[push] def getSubscriptions(userIds: Iterable[UserId], maxPerUser: Int): Fu[List[WebSubscription]] =
    coll
      .aggregateList(100_000, _.sec): framework =>
        import framework.*
        Match($doc("userId".$in(userIds))) -> List(
          Sort(Descending("seenAt")),
          GroupField("userId")("subs" -> Push(BSONString("$$ROOT"))),
          Project($doc("subs" -> Slice(BSONString("$subs"), BSONInteger(maxPerUser)), "_id" -> false)),
          Unwind("subs"),
          ReplaceRootField("subs")
        )
      .map(_.flatMap(webSubscriptionReader.readOpt))
