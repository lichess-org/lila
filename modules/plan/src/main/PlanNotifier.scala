package lila.plan

import akka.actor.*

import lila.common.Bus
import lila.core.timeline.{ Atom, Propagate }
import lila.user.User

final private[plan] class PlanNotifier(using ec: Executor, scheduler: Scheduler):

  def onCharge(user: User) =
    Bus.publish(lila.core.actorApi.plan.MonthInc(user.id, user.plan.months), "plan")
    if user.plan.months > 1 then onRenew(user) else onStart(user)

  private def onStart(user: User): Unit =
    scheduler.scheduleOnce(5 seconds) {
      Bus.publish(lila.core.actorApi.plan.PlanStart(user.id), "planStart")
    }
    pushTimeline(user)(lila.core.timeline.PlanStart.apply)

  private def onRenew(user: User): Unit =
    pushTimeline(user)(lila.core.timeline.PlanRenew(_, user.plan.months))

  def onExpire(user: User): Unit =
    Bus.publish(lila.core.actorApi.plan.PlanExpire(user.id), "planExpire")

  def onGift(from: User, to: User, isLifetime: Boolean): Unit =
    pushTimeline(to)(lila.core.timeline.PlanStart.apply)
    Bus.publish(lila.core.actorApi.plan.MonthInc(to.id, to.plan.months), "plan")
    Bus.publish(lila.core.actorApi.plan.PlanGift(from.id, to.id, isLifetime), "planStart")

  private def pushTimeline(user: User)(f: UserId => Atom): Unit =
    lila.common.Bus.named.timeline(Propagate(f(user.id)).toFollowersOf(user.id))
