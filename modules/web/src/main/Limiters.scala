package lila.web

import lila.core.net.IpAddress

object Limiters:

  val setupPost = RateLimit[IpAddress](
    5,
    1.minute,
    key = "setup.post",
    enforce = env.net.rateLimit.value,
    log = false
  )

  val AnonHookRateLimit = RateLimit.composite[IpAddress](
    key = "setup.hook.anon",
    enforce = env.net.rateLimit.value
  )(
    ("fast", 8, 1.minute),
    ("slow", 300, 1.day)
  )

  val BotAiRateLimit = RateLimit[UserId](50, 1.day, key = "setup.post.bot.ai")
