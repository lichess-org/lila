package lila.challenge

import cats.data.Validated
import cats.data.Validated.{ Invalid, Valid }

import lila.common.Bus
import lila.common.config.Max
import lila.game.{ Game, Pov }
import lila.hub.actorApi.socket.SendTo
import lila.i18n.I18nLangPicker
import lila.memo.CacheApi.*
import lila.user.{ LightUserApi, User, UserRepo }

final class ChallengeApi(
    repo: ChallengeRepo,
    challengeMaker: ChallengeMaker,
    userRepo: UserRepo,
    lightUserApi: LightUserApi,
    joiner: ChallengeJoiner,
    jsonView: JsonView,
    gameCache: lila.game.Cached,
    cacheApi: lila.memo.CacheApi
)(using
    ec: Executor,
    system: akka.actor.ActorSystem,
    scheduler: Scheduler
):

  import Challenge.*

  def allFor(userId: UserId, max: Int = 50): Fu[AllChallenges] =
    createdByDestId(userId, max) zip createdByChallengerId(userId) dmap (AllChallenges.apply).tupled

  // returns boolean success
  def create(c: Challenge): Fu[Boolean] =
    isLimitedByMaxPlaying(c) flatMap {
      if _ then fuFalse else doCreate(c) inject true
    }

  def createOpen(c: Challenge, by: Option[User]): Fu[Boolean] =
    doCreate(c) >>- by.foreach(u => openCreatedBy.put(c.id, u.id)) inject true

  private val openCreatedBy =
    cacheApi.notLoadingSync[Id, UserId](512, "challenge.open.by") {
      _.expireAfterWrite(1 hour).build()
    }

  private def doCreate(c: Challenge) =
    repo.insertIfMissing(c) >>- {
      uncacheAndNotify(c)
      Bus.publish(Event.Create(c), "challenge")
    }

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

  val countInFor = cacheApi[UserId, Int](131072, "challenge.countInFor") {
    _.expireAfterAccess(15 minutes)
      .buildAsyncFuture(repo.countCreatedByDestId)
  }

  def createdByChallengerId = repo.createdByChallengerId()

  def createdByDestId(userId: UserId, max: Int = 50) = countInFor get userId flatMap { nb =>
    if (nb > 5) repo.createdByPopularDestId(max)(userId)
    else repo.createdByDestId()(userId)
  }

  def cancel(c: Challenge) =
    repo.cancel(c) >>- {
      uncacheAndNotify(c)
      Bus.publish(Event.Cancel(c.cancel), "challenge")
    }

  private def offline(c: Challenge) = repo.offline(c) >>- uncacheAndNotify(c)

  private[challenge] def ping(id: Id): Funit =
    repo statusById id flatMap {
      case Some(Status.Created) => repo setSeen id
      case Some(Status.Offline) => repo.setSeenAgain(id) >> byId(id).map { _ foreach uncacheAndNotify }
      case _                    => fuccess(socketReload(id))
    }

  def decline(c: Challenge, reason: Challenge.DeclineReason) =
    repo.decline(c, reason) >>- {
      uncacheAndNotify(c)
      Bus.publish(Event.Decline(c declineWith reason), "challenge")
    }

  private val acceptQueue =
    lila.hub.AsyncActorSequencer(maxSize = Max(64), timeout = 5 seconds, "challengeAccept")

  def accept(
      c: Challenge,
      user: Option[User],
      sid: Option[String],
      requestedColor: Option[chess.Color] = None
  ): Fu[Validated[String, Option[Pov]]] =
    acceptQueue {
      if (c.canceled) fuccess(Invalid("The challenge has been canceled."))
      else if (c.declined) fuccess(Invalid("The challenge has been declined."))
      else if (user.exists(_.isBot) && !Game.isBotCompatible(chess.Speed(c.clock.map(_.config))))
        fuccess(Invalid("Game incompatible with a BOT account"))
      else if (c.open.exists(!_.canJoin(user))) fuccess(Invalid("The challenge is not for you to accept."))
      else
        val openFixedColor = for {
          me      <- user
          open    <- c.open
          userIds <- open.userIds
        } yield chess.Color.fromWhite(me is userIds._1)
        val color = openFixedColor orElse requestedColor
        if (c.challengerIsOpen)
          repo.setChallenger(c.setChallenger(user, sid), color) inject Valid(none)
        else if (color.map(Challenge.ColorChoice.apply).has(c.colorChoice))
          fuccess(Invalid("This color has already been chosen"))
        else
          joiner(c, user).flatMap {
            case Valid(pov) =>
              (repo accept c) >>- {
                uncacheAndNotify(c)
                Bus.publish(Event.Accept(c, user.map(_.id)), "challenge")
              } inject Valid(pov.some)
            case Invalid(err) => fuccess(Invalid(err))
          }
    }

  def offerRematchForGame(game: Game, user: User): Fu[Boolean] =
    challengeMaker.makeRematchOf(game, user) flatMapz { challenge =>
      create(challenge) recover lila.db.recoverDuplicateKey { _ =>
        logger.warn(s"${game.id} duplicate key ${challenge.id}")
        false
      }
    }

  def setDestUser(c: Challenge, u: User): Funit =
    val challenge = c setDestUser u
    repo.update(challenge) >>- {
      uncacheAndNotify(challenge)
      Bus.publish(Event.Create(challenge), "challenge")
    }

  def removeByUserId(userId: UserId) =
    repo allWithUserId userId flatMap { cs =>
      lila.common.LilaFuture.applySequentially(cs)(remove).void
    }

  def oauthAccept(dest: User, challenge: Challenge): Fu[Validated[String, Game]] =
    joiner(challenge, dest.some).map(_.map(_.game))

  private def isLimitedByMaxPlaying(c: Challenge) =
    if (c.hasClock) fuFalse
    else
      c.userIds
        .map { userId =>
          gameCache.nbPlaying(userId) dmap (lila.game.Game.maxPlaying <=)
        }
        .parallel
        .dmap(_ exists identity)

  private[challenge] def sweep: Funit =
    repo.realTimeUnseenSince(nowInstant minusSeconds 20, max = 50).flatMap { cs =>
      lila.common.LilaFuture.applySequentially(cs)(offline).void
    } >>
      repo.expired(50).flatMap { cs =>
        lila.common.LilaFuture.applySequentially(cs)(remove).void
      }

  private def remove(c: Challenge) =
    repo.remove(c.id) >>- uncacheAndNotify(c)

  private def uncacheAndNotify(c: Challenge): Unit =
    c.destUserId ?? countInFor.invalidate
    c.destUserId ?? notifyUser.apply
    c.challengerUserId ?? notifyUser.apply
    socketReload(c.id)

  private def socketReload(id: Id): Unit =
    socket.foreach(_ reload id)

  private object notifyUser:
    private val throttler = new lila.hub.EarlyMultiThrottler[UserId](logger)
    def apply(userId: UserId): Unit = throttler(userId, 3.seconds) {
      for
        all  <- allFor(userId)
        lang <- userRepo langOf userId map I18nLangPicker.byStrOrDefault
        _    <- lightUserApi.preloadMany(all.all.flatMap(_.userIds))
      yield Bus.publish(
        SendTo(userId, lila.socket.Socket.makeMessage("challenges", jsonView(all)(using lang))),
        "socketUsers"
      )
    }

  // work around circular dependency
  private var socket: Option[ChallengeSocket]               = None
  private[challenge] def registerSocket(s: ChallengeSocket) = { socket = s.some }
