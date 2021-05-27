package lila.plan

import akka.actor._
import scala.concurrent.duration._

import lila.hub.actorApi.timeline.Propagate
import lila.user.User

final private[plan] class PlanNotifier(
    timeline: lila.hub.actors.Timeline
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: ActorSystem
) {

  def onStart(user: User): Unit = {
    system.scheduler.scheduleOnce(5 seconds) {
      lila.common.Bus.publish(lila.hub.actorApi.plan.PlanStart(user.id), "planStart")
    }
    val msg = Propagate(lila.hub.actorApi.timeline.PlanStart(user.id))
    timeline ! (msg toFollowersOf user.id)
  }

  def onExpire(user: User): Unit =
    lila.common.Bus.publish(lila.hub.actorApi.plan.PlanExpire(user.id), "planExpire")
}
