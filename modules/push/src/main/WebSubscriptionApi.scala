package lila.push

import org.joda.time.DateTime

import reactivemongo.bson._

import lila.db.dsl._
import lila.user.User

final class WebSubscriptionApi(coll: Coll) {

  def getSubscriptions(max: Int)(userId: User.ID): Fu[List[WebSubscription]] =
    coll.find($doc(
      "userId" -> userId
    )).sort($doc("seenAt" -> -1)).list[Bdoc](max).map { docs =>
      docs.flatMap { doc =>
        for {
          endpoint <- doc.getAs[String]("endpoint")
          auth <- doc.getAs[String]("auth")
          p256dh <- doc.getAs[String]("p256dh")
        } yield WebSubscription(endpoint, auth, p256dh)
      }
    }

  def subscribe(user: User, subscription: WebSubscription, sessionId: String): Funit = {
    coll.update($id(sessionId), $doc(
      "userId" -> user.id,
      "endpoint" -> subscription.endpoint,
      "auth" -> subscription.auth,
      "p256dh" -> subscription.p256dh,
      "seenAt" -> DateTime.now
    ), upsert = true).void
  }

  def unsubscribeBySession(sessionId: String): Funit = {
    coll.remove($id(sessionId)).void
  }

  def unsubscribeByUser(user: User): Funit = {
    coll.remove($doc("userId" -> user.id)).void
  }

  def unsubscribeByUserExceptSession(user: User, sessionId: String): Funit = {
    coll.remove($doc(
      "userId" -> user.id,
      "_id" -> $ne(sessionId)
    )).void
  }
}
