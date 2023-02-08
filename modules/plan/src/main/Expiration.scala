package lila.plan

import lila.db.dsl.{ *, given }
import lila.user.UserRepo

import org.joda.time.DateTime

final private class Expiration(
    userRepo: UserRepo,
    patronColl: Coll,
    notifier: PlanNotifier
)(using ec: scala.concurrent.ExecutionContext):

  import BsonHandlers.given
  import BsonHandlers.PatronHandlers.given

  def run: Funit =
    getExpired flatMap {
      _.map { patron =>
        patronColl.update.one($id(patron.id), patron.removePayPal) >>
          disableUserPlanOf(patron) >>-
          logger.info(s"Expired $patron")
      }.sequenceFu.void
    }

  private def disableUserPlanOf(patron: Patron): Funit =
    userRepo byId patron.userId flatMap {
      _ ?? { user =>
        userRepo.setPlan(user, user.plan.disable) >>-
          notifier.onExpire(user)
      }
    }

  private def getExpired =
    patronColl.list[Patron](
      $doc(
        "expiresAt" $lt DateTime.now,
        "lifetime" $ne true
      ),
      50
    )
