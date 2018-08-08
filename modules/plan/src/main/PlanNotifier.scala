package lidraughts.plan

import akka.actor.ActorSelection
import scala.concurrent.duration._

import lidraughts.hub.actorApi.timeline.{ Propagate }
import lidraughts.notify.Notification.Notifies
import lidraughts.notify.{ Notification, NotifyApi }
import lidraughts.user.User

private[plan] final class PlanNotifier(
    notifyApi: NotifyApi,
    scheduler: lidraughts.common.Scheduler,
    timeline: ActorSelection
) {

  def onStart(user: User) = fuccess {
    scheduler.once(5 seconds) {
      notifyApi.addNotification(Notification.make(
        Notifies(user.id),
        lidraughts.notify.PlanStart(user.id)
      ))
    }
    val msg = Propagate(lidraughts.hub.actorApi.timeline.PlanStart(user.id))
    timeline ! (msg toFollowersOf user.id)
  }

  def onExpire(user: User) =
    notifyApi.addNotification(Notification.make(
      Notifies(user.id),
      lidraughts.notify.PlanExpire(user.id)
    ))
}
