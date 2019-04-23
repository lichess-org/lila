package lila.push

import reactivemongo.bson._

import lila.db.dsl._
import lila.user.User

private final class WebSubscriptionApi(coll: Coll) {

  // TODO: Support multiple subscriptions
  // TODO: Unsubscribe

  private implicit val WebSubscriptionBSONHandler = Macros.handler[WebSubscription]

  def getSubscriptions(userId: User.ID): Fu[List[WebSubscription]] =
    coll.find($id(userId)).list[WebSubscription](1)

  def subscribe(user: User, subscription: WebSubscription) = {
    coll.update($id(user.id), $doc(
      "_id" -> user.id,
      "endpoint" -> subscription.endpoint,
      "auth" -> subscription.auth,
      "p256dh" -> subscription.p256dh
    ), upsert = true).void
  }

  def unsubscribe(user: User, subscription: WebSubscription) = {
    coll.remove($doc(
      "_id" -> user.id,
      "auth" -> subscription.auth
    ))
  }
}
