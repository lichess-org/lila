package lila.web

import lila.core.net.IpAddress
import lila.memo.RateLimit
import lila.core.socket.Sri

final class Limiters(using Executor, lila.core.config.RateLimit):

  val setupPost = RateLimit[IpAddress](
    5,
    1.minute,
    key = "setup.post",
    log = false
  )

  val setupAnonHook = RateLimit.composite[IpAddress](
    key = "setup.hook.anon"
  )(
    ("fast", 8, 1.minute),
    ("slow", 300, 1.day)
  )

  val setupBotAi = RateLimit[UserId](50, 1.day, key = "setup.post.bot.ai")

  val boardApiConcurrency = ConcurrencyLimit[Either[Sri, UserId]](
    name = "Board API hook Stream API concurrency per user",
    key = "boardApiHook.concurrency.limit.user",
    ttl = 10.minutes,
    maxConcurrency = 1
  )

  val forumPost = RateLimit[IpAddress](
    credits = 4,
    duration = 5.minutes,
    key = "forum.post"
  )
