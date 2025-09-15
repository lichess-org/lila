package lila.plan

import lila.db.dsl.{ *, given }

final private class PlanExpiration(
    userApi: lila.core.user.UserApi,
    patronColl: Coll,
    notifier: PlanNotifier
)(using scheduler: Scheduler)(using Executor):

  import BsonHandlers.PatronHandlers.given

  scheduler.scheduleWithFixedDelay(5.minutes, 5.minutes): () =>
    run()

  private def run(): Funit =
    getExpiredUnlessLifetime.flatMap:
      _.sequentiallyVoid: patron =>
        for
          user <- userApi.byId(patron.userId).orFail(s"Missing user for $patron")
          expire = user.plan.lifetime.not
          _ <- expire.so:
            patronColl.update.one($id(patron.id), patron.removePayPal).void
          _ <- expire.so:
            disableUserPlanOf(user)
        yield if expire then logger.info(s"Expired $patron")

  private def disableUserPlanOf(user: User): Funit =
    for _ <- userApi.setPlan(user, user.plan.disable.some)
    yield notifier.onExpire(user)

  private def getExpiredUnlessLifetime =
    patronColl.list[Patron](
      dateBetween("expiresAt", nowInstant.minus(3.hours).some, nowInstant.some),
      50
    )
