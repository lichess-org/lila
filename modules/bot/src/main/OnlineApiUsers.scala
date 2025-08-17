package lila.bot

import lila.common.Bus
import lila.core.socket.{ ApiUserIsOnline, IsOnline }
import lila.memo.ExpireCallbackMemo
import lila.core.perf.UserWithPerfs

final class OnlineApiUsers(
    isOnline: IsOnline,
    cacheApi: lila.memo.CacheApi,
    userApi: lila.core.user.UserApi,
    jsonView: lila.core.user.JsonView,
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

  private def publish(userId: UserId, isOnline: Boolean) =
    Bus.pub(ApiUserIsOnline(userId, isOnline))

  private val usersCache = cacheApi.unit[List[UserWithPerfs]]:
    _.expireAfterWrite(10.seconds).buildAsyncFuture: _ =>
      userApi.visibleBotsByIds(cache.keySet)

  def getUsers = usersCache.get({})

  private val jsonCache = cacheApi.unit[String]:
    _.expireAfterWrite(10.seconds).buildAsyncFuture: _ =>
      for
        users <- getUsers
        jsons = users.map(u => jsonView.full(u.user, u.perfs.some, withProfile = true))
      yield jsons.map(play.api.libs.json.Json.stringify).mkString("\n")

  def getNdJson(nb: Option[Int]): Fu[String] =
    for
      all <- jsonCache.get({})
      lines = nb.fold(all): nb =>
        all.linesIterator.take(nb).mkString("\n")
    yield lines

object OnlineApiUsers:
  case object SetOnline
  case object CheckOnline
