package lila.plan

import akka.actor._
import scala.concurrent.duration._

import lila.common.Bus
import lila.hub.actorApi.timeline.Propagate
import lila.mon
import lila.user.User

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
    val msg = Propagate(lila.hub.actorApi.timeline.PlanStart(user.id))
    timeline ! (msg toFollowersOf user.id)
  }

  private def onRenew(user: User): Unit = {
    val msg = Propagate(lila.hub.actorApi.timeline.PlanRenew(user.id, user.plan.months))
    timeline ! (msg toFollowersOf user.id)
  }

  def onExpire(user: User): Unit =
    Bus.publish(lila.hub.actorApi.plan.PlanExpire(user.id), "planExpire")

  def onGift(from: User, to: User, isLifetime: Boolean): Unit = {
    onStart(to)
    Bus.publish(lila.hub.actorApi.plan.MonthInc(to.id, to.plan.months), "plan")
    Bus.publish(lila.hub.actorApi.plan.PlanGift(from.id, to.id), "planStart")
  }
}
