package lila.push

import reactivemongo.bson._

import lila.db.dsl._
import lila.user.User

private final class WebSubscriptionApi(coll: Coll) {

  def getSubscriptions(userId: User.ID): Fu[List[WebSubscription]] =
    // TODO: This query needs an index.
    coll.find($doc(
      "userId" -> userId
    )).list[Bdoc](3).map { docs =>
      docs.flatMap { doc =>
        for {
          endpoint <- doc.getAs[String]("_id")
          auth <- doc.getAs[String]("auth")
          p256dh <- doc.getAs[String]("p256dh")
        } yield WebSubscription(endpoint, auth, p256dh)
      }
    }

  def subscribe(user: User, subscription: WebSubscription): Funit = {
    coll.update($id(subscription.endpoint), $doc(
      "_id" -> subscription.endpoint,
      "userId" -> user.id,
      "auth" -> subscription.auth,
      "p256dh" -> subscription.p256dh
    ), upsert = true).void
  }

  def unsubscribe(user: User, subscription: WebSubscription): Funit = {
    coll.remove($doc(
      "_id" -> subscription.endpoint,
      "userId" -> user.id
    )).void
  }
}
