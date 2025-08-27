package lila.web

import lila.core.net.{ IpAddress, Bearer }
import lila.core.socket.Sri
import lila.memo.RateLimit

final class Limiters(using Executor, lila.core.config.RateLimit):

  import RateLimit.*

  val setupPost = RateLimit[IpAddress](5, 1.minute, key = "setup.post", log = false)

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

  val forumPost = RateLimit[IpAddress](credits = 4, duration = 5.minutes, key = "forum.post")
  val forumTopic = RateLimit[IpAddress](credits = 2, duration = 5.minutes, key = "forum.topic")

  val apiMe = RateLimit[UserId](30, 5.minutes, "api.account.user")
  val apiMobileHome = RateLimit[UserId | IpAddress](30, 3.minutes, "api.mobile.home")

  val apiUsers = RateLimit.composite[IpAddress](
    key = "users.api.ip"
  )(
    ("fast", 2000, 10.minutes),
    ("slow", 30000, 1.day)
  )

  val userGames = RateLimit[IpAddress](credits = 500, duration = 10.minutes, key = "user_games.web.ip")

  val crosstable = RateLimit[IpAddress](credits = 30, duration = 10.minutes, key = "crosstable.api.ip")

  val relay: RateLimiter[(UserId, IpAddress)] = combine(
    RateLimit[UserId](credits = 100 * 10, duration = 24.hour, key = "broadcast.round.user"),
    RateLimit[IpAddress](credits = 100 * 10, duration = 24.hour, key = "broadcast.round.ip")
  )

  val relayTour: RateLimiter[(UserId, IpAddress)] = combine(
    RateLimit[UserId](credits = 10 * 10, duration = 24.hour, key = "broadcast.tournament.user"),
    RateLimit[IpAddress](credits = 10 * 10, duration = 24.hour, key = "broadcast.tournament.ip")
  )

  val eventStream = RateLimit[Bearer](30, 10.minutes, "api.stream.event.bearer")

  val userActivity = RateLimit[IpAddress](credits = 15, duration = 2.minutes, key = "user_activity.api.ip")

  val magicLink = RateLimit[String](credits = 3, duration = 1.hour, key = "login.magicLink.token")

  val challenge = RateLimit[IpAddress](
    500,
    10.minute,
    key = "challenge.create.ip"
  )
  val challengeBot = RateLimit[IpAddress](
    400,
    1.day,
    key = "challenge.bot.create.ip"
  )
  val challengeUser = RateLimit.composite[UserId](
    key = "challenge.create.user"
  )(
    ("fast", 5 * 5, 1.minute),
    ("slow", 40 * 5, 1.day)
  )

  val exportImage: RateLimiter[(Unit, IpAddress)] = combine(
    RateLimit[Unit](credits = 600, duration = 1.minute, key = "export.image.global"),
    RateLimit[IpAddress](credits = 15, duration = 1.minute, key = "export.image.ip")
  )

  val gameImport = RateLimit.composite[IpAddress](
    key = "import.game.ip"
  )(
    ("fast", 10, 1.minute),
    ("slow", 150, 1.hour)
  )

  val imageUpload = RateLimit.composite[IpAddress](
    key = "image.upload.ip"
  )(
    ("fast", 10, 2.minutes),
    ("slow", 60, 1.day)
  )

  val oauthTokenTest = RateLimit[IpAddress](credits = 10_000, duration = 10.minutes, key = "api.token.test")

  val planCheckout = RateLimit.composite[lila.core.net.IpAddress](
    key = "plan.checkout.ip"
  )(
    ("fast", 8, 10.minute),
    ("slow", 40, 1.day)
  )

  val planCapture = RateLimit.composite[lila.core.net.IpAddress](
    key = "plan.capture.ip"
  )(
    ("fast", 8, 10.minute),
    ("slow", 40, 1.day)
  )

  val follow = RateLimit[UserId](credits = 150, duration = 72.hour, key = "follow.user")

  val search = RateLimit[IpAddress](credits = 50, duration = 5.minutes, key = "search.games.ip")
  val searchConcurrency = lila.web.FutureConcurrencyLimit[IpAddress](
    key = "search.games.concurrency.ip",
    ttl = 10.minutes,
    maxConcurrency = 1
  )

  val ublog = RateLimit[UserId](credits = 5 * 3, duration = 24.hour, key = "ublog.create.user")

  val tourJoinOrResume =
    RateLimit[UserId](credits = 30, duration = 10.minutes, key = "tournament.user.joinOrResume")

  val tourCreate = RateLimit[UserId](credits = 240, duration = 1.day, key = "tournament.user")

  val streamerOnlineCheck = RateLimit[UserId](1, 1.minutes, "streamer.checkOnline")

  val studyPgnImport = RateLimit[UserId](credits = 1000, duration = 24.hour, key = "study.import-pgn.user")

  val studyClone: RateLimiter[(UserId, IpAddress)] = combine(
    RateLimit[UserId](credits = 10 * 3, duration = 24.hour, key = "study.clone.user"),
    RateLimit[IpAddress](credits = 20 * 3, duration = 24.hour, key = "study.clone.ip")
  )

  val studyPgn = RateLimit[IpAddress](credits = 31, duration = 1.minute, key = "export.study.pgn.ip")

  val relayPgn = RateLimit[IpAddress](credits = 61, duration = 1.minute, key = "export.relay.pgn.ip")

  val teamKick =
    RateLimit.composite[IpAddress](key = "team.kick.api.ip")(("fast", 10, 2.minutes), ("slow", 50, 1.day))

  val openingStatsProxy = RateLimit.composite[UserId](
    key = "opening.stats.proxy.user"
  )(
    ("fast", 30, 10.minutes),
    ("slow", 100, 1.day)
  )
