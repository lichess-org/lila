package lila.bot

import scala.concurrent.duration.*

import lila.common.Bus
import lila.hub.actorApi.socket.ApiUserIsOnline
import lila.memo.ExpireCallbackMemo
import lila.socket.IsOnline
import lila.user.User

final class OnlineApiUsers(
    isOnline: IsOnline,
    scheduler: akka.actor.Scheduler
)(using scala.concurrent.ExecutionContext, play.api.Mode):

  private val cache = ExpireCallbackMemo[UserId](
    scheduler,
    10.seconds,
    userId => publish(userId, isOnline = false)
  )

  def setOnline(userId: UserId): Unit =
    val wasOffline = !isOnline.value(userId) && !cache.get(userId)
    cache.put(userId)
    if (wasOffline) publish(userId, isOnline = true)

  def get: Set[UserId] = cache.keySet

  private def publish(userId: UserId, isOnline: Boolean) =
    Bus.publish(ApiUserIsOnline(userId, isOnline), "onlineApiUsers")
