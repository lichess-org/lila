package lila.plan

import akka.actor.*

import lila.common.Bus
import lila.hub.actorApi.timeline.Propagate
import lila.user.User
import lila.hub.actorApi.timeline.Atom

final private[plan] class PlanNotifier(
    timeline: lila.hub.actors.Timeline
)(using
    ec: Executor,
    system: ActorSystem
):

  def onCharge(user: User) =
    Bus.publish(lila.hub.actorApi.plan.MonthInc(user.id, user.plan.months), "plan")
    if user.plan.months > 1 then onRenew(user) else onStart(user)

  private def onStart(user: User): Unit =
    system.scheduler.scheduleOnce(5 seconds) {
      Bus.publish(lila.hub.actorApi.plan.PlanStart(user.id), "planStart")
    }
    pushTimeline(user)(lila.hub.actorApi.timeline.PlanStart.apply)

  private def onRenew(user: User): Unit =
    pushTimeline(user)(lila.hub.actorApi.timeline.PlanRenew(_, user.plan.months))

  def onExpire(user: User): Unit =
    Bus.publish(lila.hub.actorApi.plan.PlanExpire(user.id), "planExpire")

  def onGift(from: User, to: User, isLifetime: Boolean): Unit =
    pushTimeline(to)(lila.hub.actorApi.timeline.PlanStart.apply)
    Bus.publish(lila.hub.actorApi.plan.MonthInc(to.id, to.plan.months), "plan")
    Bus.publish(lila.hub.actorApi.plan.PlanGift(from.id, to.id, isLifetime), "planStart")

  private def pushTimeline(user: User)(f: UserId => Atom): Unit =
    timeline ! (Propagate(f(user.id)) toFollowersOf user.id)
