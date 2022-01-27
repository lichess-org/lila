package lila.plan

import akka.actor._
import scala.concurrent.duration._

import lila.common.Bus
import lila.hub.actorApi.timeline.Propagate
import lila.mon
import lila.user.User
import lila.hub.actorApi.timeline.Atom

final private[plan] class PlanNotifier(
    timeline: lila.hub.actors.Timeline
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: ActorSystem
) {

  def onCharge(user: User) = {
    Bus.publish(lila.hub.actorApi.plan.MonthInc(user.id, user.plan.months), "plan")
    if (user.plan.months > 1) onRenew(user) else onStart(user)
  }

  private def onStart(user: User): Unit = {
    system.scheduler.scheduleOnce(5 seconds) {
      Bus.publish(lila.hub.actorApi.plan.PlanStart(user.id), "planStart")
    }
    pushTimeline(user)(lila.hub.actorApi.timeline.PlanStart)
  }

  private def onRenew(user: User): Unit =
    pushTimeline(user)(lila.hub.actorApi.timeline.PlanRenew(_, user.plan.months))

  def onExpire(user: User): Unit =
    Bus.publish(lila.hub.actorApi.plan.PlanExpire(user.id), "planExpire")

  def onGift(from: User, to: User, isLifetime: Boolean): Unit = {
    pushTimeline(to)(lila.hub.actorApi.timeline.PlanStart)
    Bus.publish(lila.hub.actorApi.plan.MonthInc(to.id, to.plan.months), "plan")
    Bus.publish(lila.hub.actorApi.plan.PlanGift(from.id, to.id, isLifetime), "planStart")
  }

  private def pushTimeline(user: User)(f: User.ID => Atom): Unit =
    timeline ! (Propagate(f(user.id)) toFollowersOf user.id)
}
