package lila.plan

import akka.actor._
import scala.concurrent.duration._

import lila.hub.actorApi.timeline.Propagate
import lila.notify.Notification.Notifies
import lila.notify.{ Notification, NotifyApi }
import lila.user.User

final private[plan] class PlanNotifier(
    notifyApi: NotifyApi,
    timeline: lila.hub.actors.Timeline
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: ActorSystem
) {

  def onStart(user: User) =
    fuccess {
      system.scheduler.scheduleOnce(5 seconds) {
        notifyApi
          .addNotification(
            Notification.make(
              Notifies(user.id),
              lila.notify.PlanStart(user.id)
            )
          )
          .unit
      }
      val msg = Propagate(lila.hub.actorApi.timeline.PlanStart(user.id))
      timeline ! (msg toFollowersOf user.id)
    }

  def onExpire(user: User) =
    notifyApi.addNotification(
      Notification.make(
        Notifies(user.id),
        lila.notify.PlanExpire(user.id)
      )
    )
}
