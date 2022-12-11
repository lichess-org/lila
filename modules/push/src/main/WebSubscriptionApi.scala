package lila.push

import org.joda.time.DateTime

import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }
import lila.user.User
import reactivemongo.api.ReadPreference

final class WebSubscriptionApi(coll: Coll)(using ec: scala.concurrent.ExecutionContext):

  private[push] def getSubscriptions(max: Int)(userId: UserId): Fu[List[WebSubscription]] =
    coll
      .find($doc("userId" -> userId), $doc("endpoint" -> true, "auth" -> true, "p256dh" -> true).some)
      .sort($doc("seenAt" -> -1))
      .cursor[Bdoc](ReadPreference.secondaryPreferred)
      .list(max)
      .map {
        _.flatMap { doc =>
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

  def unsubscribeBySession(sessionId: String): Funit =
    coll.delete.one($id(sessionId)).void

  def unsubscribeByUser(user: User): Funit =
    coll.delete.one($doc("userId" -> user.id)).void
