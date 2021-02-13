package lila.push

import org.joda.time.DateTime

import reactivemongo.api.bson._

import lila.db.dsl._
import lila.user.User

final class WebSubscriptionApi(coll: Coll)(implicit ec: scala.concurrent.ExecutionContext) {

  def getSubscriptions(max: Int)(userId: User.ID): Fu[List[WebSubscription]] =
    coll
      .find(
        $doc(
          "userId" -> userId
        )
      )
      .sort($doc("seenAt" -> -1))
      .cursor[Bdoc]()
      .list(max)
      .map { docs =>
        docs.flatMap { doc =>
          for {
            endpoint <- doc.string("endpoint")
            auth     <- doc.string("auth")
            p256dh   <- doc.string("p256dh")
          } yield WebSubscription(endpoint, auth, p256dh)
        }
      }

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

  def unsubscribeByUserExceptSession(user: User, sessionId: String): Funit = {
    coll.delete
      .one(
        $doc(
          "userId" -> user.id,
          "_id"    -> $ne(sessionId)
        )
      )
      .void
  }
}
