package lila.push

import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }
import lila.user.User
import reactivemongo.api.ReadPreference

final class WebSubscriptionApi(coll: Coll)(using Executor):

  import WebSubscription.given

  def subscribe(user: User, subscription: WebSubscription, sessionId: String): Funit =
    coll.update
      .one(
        $id(sessionId),
        $doc(
          "userId"    -> user.id,
          "endpoint"  -> subscription.endpoint,
          "auth"      -> subscription.auth,
          "p256dh"    -> subscription.p256dh,
          "aes128gcm" -> subscription.aes128gcm,
          "aesgcm"    -> subscription.aesgcm,
          "seenAt"    -> nowInstant
        ),
        upsert = true
      )
      .void

  def unsubscribeBySession(sessionId: String): Funit =
    coll.delete.one($id(sessionId)).void

  def unsubscribeByUser(user: User): Funit =
    coll.delete.one($doc("userId" -> user.id)).void

  private[push] def getSubscriptions(max: Int)(userId: UserId): Fu[List[WebSubscription]] =
    coll
      .find($doc("userId" -> userId))
      .sort($doc("seenAt" -> -1))
      .cursor[WebSubscription](ReadPreference.secondaryPreferred)
      .list(max)

  private[push] def getSubscriptions(userIds: Iterable[UserId], maxPerUser: Int): Fu[List[WebSubscription]] =
    coll
      .aggregateList(100_000, ReadPreference.secondaryPreferred) { framework =>
        import framework._
        Match($doc("userId" $in userIds)) -> List(
          Sort(Descending("seenAt")),
          GroupField("userId")("subs" -> Push(BSONString("$$ROOT"))),
          Project($doc("subs" -> Slice(BSONString("$subs"), BSONInteger(maxPerUser)), "_id" -> false)),
          Unwind("subs"),
          ReplaceRootField("subs")
        )
      }
      .map(_ flatMap webSubscriptionReader.readOpt)
