package lila.bot

import scala.concurrent.duration._

import lila.common.Bus
import lila.hub.actorApi.socket.ApiUserIsOnline
import lila.memo.ExpireCallbackMemo
import lila.socket.IsOnline

final class OnlineApiUsers(
    isOnline: IsOnline,
    scheduler: akka.actor.Scheduler
)(implicit ec: scala.concurrent.ExecutionContext) {

  private val cache = new ExpireCallbackMemo(
    scheduler,
    10.seconds,
    userId => publish(userId, isOnline = false)
  )

  def setOnline(userId: lila.user.User.ID): Unit = {
    val wasOffline = !isOnline(userId) && !cache.get(userId)
    cache.put(userId)
    if (wasOffline) publish(userId, isOnline = true)
  }

  def get: Set[lila.user.User.ID] = cache.keySet

  private def publish(userId: lila.user.User.ID, isOnline: Boolean) =
    Bus.publish(ApiUserIsOnline(userId, isOnline), "onlineApiUsers")
}
