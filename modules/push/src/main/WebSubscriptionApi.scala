package lila.push

import org.joda.time.DateTime

import reactivemongo.api.bson._

import lila.db.dsl._
import lila.user.User
import reactivemongo.api.ReadPreference

final class WebSubscriptionApi(coll: Coll)(implicit ec: scala.concurrent.ExecutionContext) {

  def subscribe(user: User, subscription: WebSubscription, sessionId: String): Funit =
    coll.update
      .one(
        $id(sessionId),
        $doc(
          "userId"   -> user.id,
          "endpoint" -> subscription.endpoint,
          "auth"     -> subscription.auth,
          "p256dh"   -> subscription.p256dh,
          "seenAt"   -> DateTime.now
        ),
        upsert = true
      )
      .void

  def unsubscribeBySession(sessionId: String): Funit = {
    coll.delete.one($id(sessionId)).void
  }

  def unsubscribeByUser(user: User): Funit = {
    coll.delete.one($doc("userId" -> user.id)).void
  }

  private[push] def getSubscriptions(max: Int)(userId: User.ID): Fu[List[WebSubscription]] =
    coll
      .find($doc("userId" -> userId), $doc("endpoint" -> true, "auth" -> true, "p256dh" -> true).some)
      .sort($doc("seenAt" -> -1))
      .cursor[Bdoc](ReadPreference.secondaryPreferred)
      .list(max)
      .map(_ flatMap bsonToWebSub)

  private[push] def getSubscriptions(userIds: Iterable[User.ID], maxPerUser: Int): Fu[List[WebSubscription]] =
    coll
      .aggregateList(100000, ReadPreference.secondaryPreferred) { framework =>
        import framework._
        Match($doc("userId" -> $doc("$in" -> userIds))) -> List(
          Sort(Descending("seenAt")),
          Group($id("userId" -> "$userId"))("subs" -> AddToSet(BSONString("$$ROOT"))),
          Project($doc("subs" -> Slice(BSONString("$subs"), BSONInteger(maxPerUser)), "_id" -> false)),
          Unwind("subs"),
          ReplaceRootField("subs")
        )
      }
      .map(_ flatMap bsonToWebSub)

  private def bsonToWebSub(doc: Bdoc) =
    for {
      endpoint <- doc.string("endpoint")
      auth     <- doc.string("auth")
      p256dh   <- doc.string("p256dh")
    } yield WebSubscription(endpoint, auth, p256dh)
}
