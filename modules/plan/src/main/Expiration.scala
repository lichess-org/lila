package lila.plan

import lila.db.dsl.{ *, given }
import lila.user.UserRepo

final private class Expiration(
    userRepo: UserRepo,
    patronColl: Coll,
    notifier: PlanNotifier
)(using Executor):

  import BsonHandlers.PatronHandlers.given

  def run: Funit =
    getExpired.flatMap:
      _.traverse_ { patron =>
        patronColl.update.one($id(patron.id), patron.removePayPal) >>
          disableUserPlanOf(patron) andDo
          logger.info(s"Expired $patron")
      }

  private def disableUserPlanOf(patron: Patron): Funit =
    userRepo byId patron.userId flatMapz { user =>
      userRepo.setPlan(user, user.plan.disable) andDo
        notifier.onExpire(user)
    }

  private def getExpired =
    patronColl.list[Patron](
      $doc(
        "expiresAt" $lt nowInstant,
        "lifetime" $ne true
      ),
      50
    )
