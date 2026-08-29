package lila.web

import play.api.mvc.RequestHeader
import scalalib.net.Bearer
import chess.format.pgn.PgnStr

import lila.core.net.IpAddress
import lila.core.socket.Sri
import lila.core.security.IsProxy
import lila.memo.RateLimit
import lila.common.{ HTTPRequest, ClientName }
import lila.ui.Context
import lila.core.perm.Granter

final class Limiters(using Executor, lila.core.config.RateLimit):

  import RateLimit.*

  private given (using ctx: Context): RequestHeader = ctx.req

  val setupPost = RateLimit[IpAddress](5, 1.minute, key = "setup.post", log = false)

  val setupAnonHook = RateLimit.composite[IpAddress](
    key = "setup.hook.anon"
  )(
    ("fast", 8, 1.minute),
    ("slow", 300, 1.day)
  )

  val setupBotAi = RateLimit[UserId](20, 1.day, key = "setup.post.bot.ai")

  val boardApiConcurrency = ConcurrencyLimit[Either[Sri, UserId]](
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

  val eventStream = RateLimit[Bearer](30, 10.minutes, "api.stream.event.bearer")

  val userActivity = RateLimit[IpAddress](credits = 15, duration = 2.minutes, key = "user_activity.api.ip")

  val magicLink = RateLimit[String](credits = 3, duration = 1.hour, key = "login.magicLink.token")

  val challenge = RateLimit[IpAddress](
    500,
    10.minute,
    key = "challenge.create.ip"
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

  private val imageUploadLimiter: RateLimiter[(IpAddress, UserId)] = combine(
    RateLimit.composite[IpAddress](key = "image.upload.ip")(
      ("fast", 10, 2.minutes),
      ("slow", 60, 1.day)
    ),
    RateLimit.composite[UserId](key = "image.upload.user")(
      ("fast", 10, 5.minutes),
      ("slow", 30, 1.day)
    )
  )
  def imageUpload[A](limited: => Fu[A])(using ctx: Context, me: Me) =
    imageUploadLimiter(ctx.ip -> me.userId, limited)

  val preview = RateLimit[UserId](30, 3.minutes, "preview.user")

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

  val recapAwaitConcurrency = lila.web.FutureConcurrencyLimit[UserId](
    key = "recap.await.concurrency.user",
    ttl = 3.minutes,
    maxConcurrency = 2
  )

  val ublog = RateLimit[UserId](credits = 5 * 3, duration = 24.hour, key = "ublog.create.user")

  val tourJoinOrResume =
    RateLimit[UserId](credits = 30, duration = 10.minutes, key = "tournament.user.joinOrResume")

  val tourCreate = RateLimit[UserId](credits = 240, duration = 1.day, key = "tournament.user")

  val streamerOnlineCheck: RateLimiter[(UserId, IpAddress)] = combine(
    RateLimit[UserId](1, 1.minute, "streamer.checkOnline.user"),
    RateLimit[IpAddress](1, 1.minute, "streamer.checkOnline.ip")
  )

  val studyPgnImport = RateLimit[UserId](credits = 1000, duration = 24.hour, key = "study.import-pgn.user")

  val studyClone: RateLimiter[(UserId, IpAddress)] = combine(
    RateLimit[UserId](credits = 10 * 3, duration = 24.hour, key = "study.clone.user"),
    RateLimit[IpAddress](credits = 20 * 3, duration = 24.hour, key = "study.clone.ip")
  )

  val studyCreate: RateLimiter[(UserId, IpAddress)] = combine(
    RateLimit[UserId](credits = 30 * 2, duration = 24.hour, key = "study.create.user"),
    RateLimit[IpAddress](credits = 50 * 2, duration = 24.hour, key = "study.create.ip")
  )

  object studyDownload:
    private val auth = ConcurrencyLimit[UserId](3, "study.download.auth")
    private val anon = ConcurrencyLimit[IpAddress](1, "study.download.anon")
    def perSecond(using ctx: Context) = if ctx.isAuth then 30 else 10
    def apply[T]()(using ctx: Context): ConcurrencyLimit.Limiter[PgnStr] =
      ctx.userId.fold(anon(ctx.ip))(auth(_))

  val teamKick =
    RateLimit.composite[IpAddress](key = "team.kick.api.ip")(("fast", 10, 2.minutes), ("slow", 50, 1.day))

  val coachSearch = RateLimit[UserId](credits = 15 * 3, duration = 10.minutes, key = "coach.search.user")

  object relay:

    val roundCreate: RateLimiter[(UserId, IpAddress)] = combine(
      RateLimit[UserId](120 * 10, 24.hour, "broadcast.round.user"),
      RateLimit[IpAddress](120 * 10, 24.hour, "broadcast.round.ip")
    )

    val tourCreate: RateLimiter[(UserId, IpAddress)] = combine(
      RateLimit[UserId](20 * 10, 24.hour, "broadcast.tournament.user"),
      RateLimit[IpAddress](20 * 10, 24.hour, "broadcast.tournament.ip")
    )

    object stream:
      private val auth = ConcurrencyLimit[UserId](8, "broadcast.stream.auth")
      private val verified = ConcurrencyLimit[UserId](32, "broadcast.stream.verified")
      private val anon = ConcurrencyLimit[IpAddress](2, "broadcast.stream.anon")
      def apply[T]()(using ctx: Context): ConcurrencyLimit.Limiter[PgnStr] = ctx.me match
        case None => anon(ctx.ip)
        case Some(me) if me.isVerified => verified(me.userId)
        case Some(me) => auth(me.userId)

    object apiGet:
      private val authLimit = 120
      private val auth = RateLimit[UserId]((authLimit * 10 * 3), 3.minutes, "broadcast.api.get.auth")
      private val mobile = RateLimit[IpAddress](20 * 3, 3.minutes, "broadcast.api.get.mobile")
      private val anon = RateLimit[IpAddress](10 * 3, 3.minutes, "broadcast.api.get.anon")
      def apply[A](limited: String => Fu[A])(using ctx: Context, client: ClientName)(res: => Fu[A]): Fu[A] =
        ctx.me match
          case Some(me) =>
            val cost = if Granter.of(_.ApiHog)(me) then 2 else if me.isVerified then 5 else 10
            auth(me.userId, limited(limitMessage), cost)(res)
          case None if client.isMobile => mobile(ctx.ip, limited(limitMessage))(res)
          case None => anon(ctx.ip, limited(limitMessage))(res)
      private def limitMessage(using ctx: Context) =
        if ctx.isAuth then "Too many requests from your account"
        else s"Use an oauth token to get $authLimit requests per minute. Contact us for more."

  object enumeration:

    private val maxCost = 3

    private val openingLimiter = RateLimit[IsProxy](15 * maxCost, 1.minute, "opening.byKeyAndMoves.proxy")
    def opening[A]: ProxyLimit[A] = proxyLimit(openingLimiter)

    private val userProfileLimiter = RateLimit[IsProxy](60 * maxCost, 1.minute, "user.profile.page.proxy")
    def userProfile[A]: ProxyLimitWithUri[A] = proxyLimitWithUri(userProfileLimiter)

    private val searchLimiter = RateLimit[IsProxy](15 * maxCost, 1.minute, "search.proxy")
    def search[A]: ProxyLimit[A] = proxyLimit(searchLimiter)

    private val cloudEvalLimiter = RateLimit[IsProxy](30 * maxCost, 1.minute, "cloudEval.proxy")
    def cloudEval[A]: ProxyLimit[A] = proxyLimit(cloudEvalLimiter)

    private val fidePlayerLimiter = RateLimit[IsProxy](60 * maxCost, 1.minute, "fide.player.proxy")
    def fidePlayer[A]: ProxyLimit[A] = proxyLimit(fidePlayerLimiter)

    private val signupLimiter = RateLimit[IsProxy](20 * maxCost, 1.minute, "user.signup.proxy")
    def signup[A]: ProxyLimit[A] = proxyLimit(signupLimiter, flatCost(maxCost))

    private type ProxyLimit[A] = (IsProxy, RequestHeader, Option[Me]) ?=> (=> Fu[A]) => (=> Fu[A]) => Fu[A]
    private type ProxyLimitWithUri[A] =
      (IsProxy, RequestHeader, Option[Me]) ?=> (=> Fu[A]) => (=> Fu[A]) => Fu[A]

    private def proxyLimit[A](
        limiter: RateLimiter[IsProxy],
        cost: IsProxy ?=> Int = defaultCost
    ): ProxyLimit[A] =
      (proxy, req, me) ?=>
        default =>
          f =>
            if proxy.no || me.isDefined then f
            else limiter(proxy, default, cost, msg = HTTPRequest.ipAddressStr(req))(f)

    private def proxyLimitWithUri[A](
        limiter: RateLimiter[IsProxy],
        cost: IsProxy ?=> Int = defaultCost
    ): ProxyLimitWithUri[A] =
      (proxy, req, me) ?=>
        default =>
          f =>
            if proxy.no || me.isDefined then f
            else limiter(proxy, default, cost, msg = s"${HTTPRequest.ipAddressStr(req)} ${req.uri}")(f)

    private def defaultCost(using proxy: IsProxy): Int =
      if proxy.isFloodish then maxCost
      else if proxy.isVpn then 1
      else 0

    private def flatCost(cost: Int)(using proxy: IsProxy): Int =
      if proxy.yes then cost else 0
