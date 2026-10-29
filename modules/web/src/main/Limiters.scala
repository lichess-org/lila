package lila.web

import chess.format.pgn.PgnStr
import play.api.mvc.RequestHeader

import lila.common.{ ClientName, HTTPRequest }
import lila.core.net.{ ASN, IpAddress }
import lila.core.perm.Granter
import lila.core.security.IsProxy
import lila.core.socket.Sri
import lila.memo.RateLimit
import lila.ui.Context

final class Limiters(using Executor, lila.core.config.RateLimit):

  import RateLimit.*

  private given (using ctx: Context): RequestHeader = ctx.req

  object GlobalConcurrencyLimitPerIP:

    def events(using ctx: Context): ConcurrencyLimit[IpAddress] =
      if !ctx.isAuth then eventsForAnon
      else if ctx.me.exists(_.isVerified) then eventsForVerifiedUser
      else eventsForUser

    def download(using ctx: Context): ConcurrencyLimit[IpAddress] =
      if ctx.me.exists(_.isVerified) then downloadForVerifiedUser
      else downloadBase

    private val eventsForAnon = ConcurrencyLimit[IpAddress](4, "api.ip.events.anon")
    private val eventsForUser = ConcurrencyLimit[IpAddress](8, "api.ip.events.user")
    private val eventsForVerifiedUser = ConcurrencyLimit[IpAddress](16, "api.ip.events.verified")

    private val downloadBase = ConcurrencyLimit[IpAddress](2, "api.ip.download")
    private val downloadForVerifiedUser = ConcurrencyLimit[IpAddress](8, "api.ip.download")
    val generous = ConcurrencyLimit[IpAddress](20, "api.ip.generous")

  val setupPost = RateLimit[IpAddress](5, 1.minute, "setup.post", log = false)

  val setupAnonHook = RateLimit
    .composite[IpAddress](
      key = "setup.hook.anon"
    )(
      ("fast", 8, 1.minute),
      ("slow", 300, 1.day)
    )
    .ctxCost: me ?=>
      me.isEmpty.so(1)

  val setupBotAi = RateLimit[UserId](20, 1.day, "setup.post.bot.ai").ctxCost: me ?=>
    me.exists(_.isBot).so(1)

  val boardApiConcurrency = ConcurrencyLimit[Either[Sri, UserId]](
    key = "boardApiHook.concurrency.limit.user",
    ttl = 10.minutes,
    maxConcurrency = 1
  )

  val forumPost = RateLimit[IpAddress](4, 5.minutes, "forum.post")
  val forumTopic = RateLimit[IpAddress](2, 5.minutes, "forum.topic")

  val apiMe = RateLimit[UserId](30, 5.minutes, "api.account.user")
  val apiMobileHome = RateLimit[UserId | IpAddress](30, 3.minutes, "api.mobile.home")

  val apiUsers = RateLimit.composite[IpAddress](
    key = "users.api.ip"
  )(
    ("fast", 2000, 10.minutes),
    ("slow", 30000, 1.day)
  )

  val userGames = RateLimit[IpAddress](500, 10.minutes, "user_games.web.ip")

  val crosstable = RateLimit[IpAddress](30, 10.minutes, "crosstable.api.ip")

  val eventStream = RateLimit[UserId](30, 10.minutes, "api.stream.event.bearer", log = false)

  val userActivity = RateLimit[IpAddress](15, 2.minutes, "user_activity.api.ip")

  val challenge = RateLimit[IpAddress](500, 10.minute, "challenge.create.ip")
  val challengeUser = RateLimit.composite[UserId](
    key = "challenge.create.user"
  )(
    ("fast", 5 * 5, 1.minute),
    ("slow", 40 * 5, 1.day)
  )

  val exportImage: RateLimiter[(Unit, IpAddress)] = combine(
    RateLimit[Unit](600, 1.minute, "export.image.global"),
    RateLimit[IpAddress](15, 1.minute, "export.image.ip")
  )

  val gameImport = RateLimit
    .composite[IpAddress](
      key = "import.game.ip"
    )(
      ("fast", 10, 1.minute),
      ("slow", 150, 1.hour)
    )
    .ctxCost: me ?=>
      if me.isDefined then 1 else 2

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

  val oauthTokenTest = RateLimit[IpAddress](10_000, 10.minutes, "api.token.test")

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

  val follow = RateLimit[UserId](150, 72.hour, "follow.user")

  val search = RateLimit[UserId](20, 5.minutes, "search.games.user")
  val searchConcurrency = lila.web.FutureConcurrencyLimit[UserId](
    key = "search.games.concurrency.user",
    ttl = 10.minutes,
    maxConcurrency = 1
  )

  val recapAwaitConcurrency = lila.web.FutureConcurrencyLimit[UserId](
    key = "recap.await.concurrency.user",
    ttl = 3.minutes,
    maxConcurrency = 2
  )

  val tourJoinOrResume =
    RateLimit[UserId](30, 10.minutes, "tournament.user.joinOrResume")

  val tourCreate = RateLimit[UserId](240, 1.day, "tournament.user").myCostAdjusted: me ?=>
    if me.is(UserId.lichess) then 1
    else if Granter(_.ManageTournament) then 2
    else 20

  val streamerOnlineCheck: RateLimiter[(UserId, IpAddress)] = combine(
    RateLimit[UserId](1, 1.minute, "streamer.checkOnline.user"),
    RateLimit[IpAddress](1, 1.minute, "streamer.checkOnline.ip")
  )

  val studyPgnImport = RateLimit[UserId](1000, 24.hour, "study.import-pgn.user")

  val studyClone: RateLimiter[(UserId, IpAddress)] = combine(
    RateLimit[UserId](10 * 3, 24.hour, "study.clone.user"),
    RateLimit[IpAddress](20 * 3, 24.hour, "study.clone.ip")
  )

  val studyCreate: RateLimiter[(UserId, IpAddress)] = combine(
    RateLimit[UserId](30 * 2, 24.hour, "study.create.user"),
    RateLimit[IpAddress](50 * 2, 24.hour, "study.create.ip")
  )

  val anyPageByUser = RateLimit[MyId](40, 2.minutes, "page.user")

  object studyDownload:
    private val auth = ConcurrencyLimit[UserId](3, "study.download.auth")
    private val anon = ConcurrencyLimit[IpAddress](1, "study.download.anon")
    def perSecond(using ctx: Context) = if ctx.isAuth then 30 else 10
    def apply[T]()(using ctx: Context): ConcurrencyLimit.Limiter[PgnStr] =
      ctx.userId.fold(anon(ctx.ip))(auth(_))

  val teamKick =
    RateLimit.composite[MyId](key = "team.kick.api.user")(("fast", 10, 2.minutes), ("slow", 50, 1.day))

  def userProfileLimiter[A](rateLimited: => Fu[A])(using
      me: Option[Me]
  )(using IsProxy, Context)(op: => Fu[A]): Fu[A] =
    enumeration.userProfile(rateLimited):
      val myCost = if Granter.opt(_.UserModView) then 0 else 1
      anyPageByUser.option(me.map(_.myId), rateLimited, myCost)(op)

  object relay:

    val roundCreate = combine(
      RateLimit[UserId](120 * 10, 24.hour, "broadcast.round.user"),
      RateLimit[IpAddress](120 * 10, 24.hour, "broadcast.round.ip")
    ).myCost: me ?=>
      if Granter(_.StudyAdmin) then 1
      else if Granter(_.Relay) then 2
      else if me.hasTitle || me.isVerified then 5
      else 10

    val tourCreate = combine(
      RateLimit[UserId](20 * 10, 24.hour, "broadcast.tournament.user"),
      RateLimit[IpAddress](20 * 10, 24.hour, "broadcast.tournament.ip")
    ).myCost: me ?=>
      if Granter(_.StudyAdmin) then 1
      else if Granter(_.Relay) then 2
      else if me.hasTitle || me.isVerified then 5
      else 10

    val push = RateLimit[UserId](120 * 3, 1.minutes, "broadcast.round.push").myCost:
      if Granter(_.Relay) then 1 else 3

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
      def apply[A](limited: String => Fu[A], cost: Int = 1)(using
          ctx: Context,
          client: ClientName
      )(res: => Fu[A]): Fu[A] =
        ctx.me match
          case Some(me) =>
            val finalCost = cost * {
              if Granter.of(_.ApiHog)(me) then 2 else if me.isVerified then 5 else 10
            }
            auth(me.userId, limited(limitMessage), finalCost)(res)
          case None if client.isMobile => mobile(ctx.ip, limited(limitMessage), cost)(res)
          case None => anon(ctx.ip, limited(limitMessage), cost)(res)
      private def limitMessage(using ctx: Context) =
        if ctx.isAuth then "Too many requests from your account"
        else s"Use an oauth token to get $authLimit requests per minute. Contact us for more."

  object enumeration:

    private val maxCost = 3

    private val openingLimiter = RateLimit[ASN](15 * maxCost, 1.minute, "opening.byKeyAndMoves.proxy")
    def opening[A]: AsnLimit[A] = asnLimit(openingLimiter)

    private val userProfileLimiter = RateLimit[ASN](60 * maxCost, 1.minute, "user.profile.page.proxy")
    def userProfile[A]: AsnLimit[A] = asnLimit(userProfileLimiter)

    private val searchLimiter = RateLimit[ASN](15 * maxCost, 1.minute, "search.proxy")
    def search[A]: AsnLimit[A] = asnLimit(searchLimiter)

    private val cloudEvalLimiter = RateLimit[ASN](30 * maxCost, 1.minute, "cloudEval.proxy")
    def cloudEval[A]: AsnLimit[A] = asnLimit(cloudEvalLimiter)

    private val fidePlayerLimiter = RateLimit[ASN](60 * maxCost, 1.minute, "fide.player.proxy")
    def fidePlayer[A]: AsnLimit[A] = asnLimit(fidePlayerLimiter)

    private val signupLimiter = RateLimit[ASN](20 * maxCost, 1.minute, "user.signup.proxy")
    def signup[A]: AsnLimit[A] = asnLimit(signupLimiter, flatCost(maxCost))

    private type AsnLimit[A] = (IsProxy, RequestHeader, Option[MyId]) ?=> (=> Fu[A]) => (=> Fu[A]) => Fu[A]

    private def asnLimit[A](
        limiter: RateLimiter[ASN],
        cost: IsProxy ?=> Int = defaultCost
    ): AsnLimit[A] =
      (proxy, req, me) ?=>
        default =>
          f =>
            if proxy.no || me.isDefined || HTTPRequest.nginxWhitelist(req) then f
            else HTTPRequest.asn(req).fold(f)(asn => limiter(asn, default, cost, msg = s"ASN $asn")(f))

    private def defaultCost(using proxy: IsProxy): Int =
      if proxy.isFloodish then maxCost
      else if proxy.isVpn then 1
      else 0

    private def flatCost(cost: Int)(using proxy: IsProxy): Int =
      if proxy.yes then cost else 0
