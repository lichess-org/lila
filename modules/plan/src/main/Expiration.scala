package lila.plan

import lila.db.dsl.{ *, given }

final private class Expiration(
    userApi: lila.core.user.UserApi,
    patronColl: Coll,
    notifier: PlanNotifier
)(using Executor):

  import BsonHandlers.PatronHandlers.given

  def run: Funit =
    getExpired.flatMap:
      _.sequentiallyVoid: patron =>
        for
          _ <- patronColl.update.one($id(patron.id), patron.removePayPal)
          _ <- disableUserPlanOf(patron)
        yield logger.info(s"Expired $patron")

  private def disableUserPlanOf(patron: Patron): Funit =
    userApi.byId(patron.userId).flatMapz { user =>
      for _ <- userApi.setPlan(user, user.plan.disable.some) yield notifier.onExpire(user)
    }

  private def getExpired =
    patronColl.list[Patron](
      $doc(
        "expiresAt".$lt(nowInstant),
        "lifetime".$ne(true)
      ),
      50
    )
