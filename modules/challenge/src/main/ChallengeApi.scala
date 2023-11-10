package lila.challenge

import lila.common.Bus
import lila.common.config.Max
import lila.game.{ Game, Pov }
import lila.hub.actorApi.socket.SendTo
import lila.i18n.I18nLangPicker
import lila.memo.CacheApi.*
import lila.user.{ Me, LightUserApi, User, UserRepo, UserPerfsRepo }

final class ChallengeApi(
    repo: ChallengeRepo,
    challengeMaker: ChallengeMaker,
    userRepo: UserRepo,
    perfsRepo: UserPerfsRepo,
    lightUserApi: LightUserApi,
    joiner: ChallengeJoiner,
    jsonView: JsonView,
    gameCache: lila.game.Cached,
    cacheApi: lila.memo.CacheApi
)(using Executor, akka.actor.ActorSystem, Scheduler):

  import Challenge.*

  def allFor(userId: UserId, max: Int = 50): Fu[AllChallenges] =
    createdByDestId(userId, max) zip createdByChallengerId(userId) dmap (AllChallenges.apply).tupled

  // returns boolean success
  def create(c: Challenge): Fu[Boolean] =
    isLimitedByMaxPlaying(c) flatMap:
      if _ then fuFalse else doCreate(c) inject true

  def createOpen(config: lila.setup.OpenConfig)(using me: Option[Me]): Fu[Challenge] =
    val c = Challenge.make(
      variant = config.variant,
      initialFen = config.position,
      timeControl = TimeControl.make(config.clock, config.days),
      mode = chess.Mode(config.rated),
      color = "random",
      challenger = Challenger.Open,
      destUser = none,
      rematchOf = none,
      name = config.name,
      openToUserIds = config.userIds,
      rules = config.rules,
      expiresAt = config.expiresAt
    )
    doCreate(c) andDo me.foreach(me => openCreatedBy.put(c.id, me)) inject c

  private val openCreatedBy =
    cacheApi.notLoadingSync[Id, UserId](512, "challenge.open.by"):
      _.expireAfterWrite(1 hour).build()

  private def doCreate(c: Challenge) =
    repo.insertIfMissing(c) andDo:
      uncacheAndNotify(c)
      Bus.publish(Event.Create(c), "challenge")

  def isOpenBy(id: Id, maker: User) = openCreatedBy.getIfPresent(id).contains(maker.id)

  export repo.byId

  def activeByIdFor(id: Id, dest: User): Future[Option[Challenge]] =
    repo.byIdFor(id, dest).dmap(_.filter(_.active))
  def activeByIdBy(id: Id, maker: User): Future[Option[Challenge]] =
    repo
      .byId(id)
      .dmap(_.filter { c =>
        c.active && c.challenger.match
          case Challenger.Registered(orig, _) if maker is orig => true
          case Challenger.Open if isOpenBy(id, maker)          => true
          case _                                               => false
      })

  val countInFor = cacheApi[UserId, Int](131072, "challenge.countInFor"):
    _.expireAfterAccess(15 minutes).buildAsyncFuture(repo.countCreatedByDestId)

  def createdByChallengerId = repo.createdByChallengerId()

  def createdByDestId(userId: UserId, max: Int = 50) = countInFor get userId flatMap: nb =>
    if nb > 5 then repo.createdByPopularDestId(max)(userId)
    else repo.createdByDestId()(userId)

  def cancel(c: Challenge) =
    repo.cancel(c) andDo:
      uncacheAndNotify(c)
      Bus.publish(Event.Cancel(c.cancel), "challenge")

  private def offline(c: Challenge) = repo.offline(c) andDo uncacheAndNotify(c)

  private[challenge] def ping(id: Id): Funit =
    repo statusById id flatMap:
      case Some(Status.Created) => repo setSeen id
      case Some(Status.Offline) => repo.setSeenAgain(id) >> byId(id).map { _ foreach uncacheAndNotify }
      case _                    => fuccess(socketReload(id))

  def decline(c: Challenge, reason: Challenge.DeclineReason) =
    repo.decline(c, reason) andDo {
      uncacheAndNotify(c)
      Bus.publish(Event.Decline(c declineWith reason), "challenge")
    }

  private val acceptQueue =
    lila.hub.AsyncActorSequencer(maxSize = Max(64), timeout = 5 seconds, "challengeAccept")

  def accept(
      c: Challenge,
      sid: Option[String],
      requestedColor: Option[chess.Color] = None
  )(using me: Option[Me]): Fu[Either[String, Option[Pov]]] =
    acceptQueue:
      def withPerf = me.map(_.value).soFu(perfsRepo.withPerf(_, c.perfType))
      if c.canceled
      then fuccess(Left("The challenge has been canceled."))
      else if c.declined
      then fuccess(Left("The challenge has been declined."))
      else if me.exists(_.isBot) && !Game.isBotCompatible(chess.Speed(c.clock.map(_.config)))
      then fuccess(Left("Game incompatible with a BOT account"))
      else if c.open.exists(!_.canJoin)
      then fuccess(Left("The challenge is not for you to accept."))
      else
        val openFixedColor = for
          me      <- me
          open    <- c.open
          userIds <- open.userIds
        yield chess.Color.fromWhite(me is userIds._1)
        val color = openFixedColor orElse requestedColor
        if c.challengerIsOpen
        then
          withPerf.flatMap: me =>
            repo.setChallenger(c.setChallenger(me, sid), color) inject none.asRight
        else if color.map(Challenge.ColorChoice.apply).has(c.colorChoice)
        then fuccess(Left("This color has already been chosen"))
        else
          for
            me   <- withPerf
            join <- joiner(c, me)
            result <- join match
              case Right(pov) =>
                repo.accept(c) andDo {
                  uncacheAndNotify(c)
                  Bus.publish(Event.Accept(c, me.map(_.id)), "challenge")
                } inject Right(pov.some)
              case Left(err) => fuccess(Left(err))
          yield result

  def offerRematchForGame(game: Game, user: User): Fu[Boolean] =
    challengeMaker.makeRematchOf(game, user) flatMapz { challenge =>
      create(challenge) recover lila.db.recoverDuplicateKey: _ =>
        logger.warn(s"${game.id} duplicate key ${challenge.id}")
        false
    }

  def setDestUser(c: Challenge, u: User): Funit = for
    user <- perfsRepo.withPerf(u, c.perfType)
    challenge = c setDestUser user
    _ <- repo.update(challenge)
  yield
    uncacheAndNotify(challenge)
    Bus.publish(Event.Create(challenge), "challenge")

  def removeByUserId(userId: UserId): Funit =
    repo.allWithUserId(userId).flatMap(_.traverse_(remove)).void

  private def isLimitedByMaxPlaying(c: Challenge) =
    if c.hasClock then fuFalse
    else
      c.userIds
        .map: userId =>
          gameCache.nbPlaying(userId).dmap(lila.game.Game.maxPlaying <=)
        .parallel
        .dmap(_ exists identity)

  private[challenge] def sweep: Funit =
    repo
      .realTimeUnseenSince(nowInstant minusSeconds 20, max = 50)
      .flatMap(_.traverse_(offline)) >>
      repo.expired(50).flatMap(_.traverse_(remove))

  private def remove(c: Challenge) =
    repo.remove(c.id) andDo uncacheAndNotify(c)

  private def uncacheAndNotify(c: Challenge): Unit =
    c.destUserId foreach countInFor.invalidate
    c.destUserId foreach notifyUser.apply
    c.challengerUserId foreach notifyUser.apply
    socketReload(c.id)

  private def socketReload(id: Id): Unit =
    socket.foreach(_ reload id)

  private object notifyUser:
    private val throttler = new lila.hub.EarlyMultiThrottler[UserId](logger)
    def apply(userId: UserId): Unit = throttler(userId, 3.seconds):
      for
        all  <- allFor(userId)
        lang <- userRepo langOf userId map I18nLangPicker.byStrOrDefault
        _    <- lightUserApi.preloadMany(all.all.flatMap(_.userIds))
      yield Bus.publish(
        SendTo(userId, lila.socket.Socket.makeMessage("challenges", jsonView(all)(using lang))),
        "socketUsers"
      )

  // work around circular dependency
  private var socket: Option[ChallengeSocket]               = None
  private[challenge] def registerSocket(s: ChallengeSocket) = socket = s.some
