package lila.bot

import lila.common.Bus
import lila.core.socket.{ ApiUserIsOnline, IsOnline }
import lila.memo.ExpireCallbackMemo

final class OnlineApiUsers(
    isOnline: IsOnline,
    scheduler: Scheduler
)(using Executor):

  private val cache = ExpireCallbackMemo[UserId](
    scheduler,
    10.seconds,
    userId => publish(userId, isOnline = false)
  )

  def setOnline(userId: UserId): Unit =
    val wasOffline = !isOnline.exec(userId) && !cache.get(userId)
    cache.put(userId)
    if wasOffline then publish(userId, isOnline = true)

  def get: Set[UserId] = cache.keySet

  private def publish(userId: UserId, isOnline: Boolean) =
    Bus.publish(ApiUserIsOnline(userId, isOnline), "onlineApiUsers")
