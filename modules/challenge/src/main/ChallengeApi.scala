package lila.challenge

import cats.data.Validated
import cats.data.Validated.{ Invalid, Valid }
import org.joda.time.DateTime
import scala.concurrent.duration._

import lila.common.Bus
import lila.common.config.Max
import lila.game.{ Game, Pov }
import lila.hub.actorApi.socket.SendTo
import lila.i18n.I18nLangPicker
import lila.memo.CacheApi._
import lila.memo.ExpireSetMemo
import lila.user.{ User, UserRepo }

final class ChallengeApi(
    repo: ChallengeRepo,
    challengeMaker: ChallengeMaker,
    userRepo: UserRepo,
    joiner: ChallengeJoiner,
    jsonView: JsonView,
    gameCache: lila.game.Cached,
    cacheApi: lila.memo.CacheApi
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: akka.actor.ActorSystem
) {

  import Challenge._

  def allFor(userId: User.ID, max: Int = 50): Fu[AllChallenges] =
    createdByDestId(userId, max) zip createdByChallengerId(userId) dmap (AllChallenges.apply _).tupled

  // returns boolean success
  def create(c: Challenge): Fu[Boolean] =
    isLimitedByMaxPlaying(c) flatMap {
      case true => fuFalse
      case false =>
        repo.insertIfMissing(c) >>- {
          uncacheAndNotify(c)
          Bus.publish(Event.Create(c), "challenge")
        } inject true
    }

  def byId = repo byId _

  def activeByIdFor(id: Challenge.ID, dest: User) = repo.byIdFor(id, dest).dmap(_.filter(_.active))
  def activeByIdBy(id: Challenge.ID, orig: User)  = repo.byIdBy(id, orig).dmap(_.filter(_.active))

  val countInFor = cacheApi[User.ID, Int](131072, "challenge.countInFor") {
    _.expireAfterAccess(15 minutes)
      .buildAsyncFuture(repo.countCreatedByDestId)
  }

  def createdByChallengerId = repo.createdByChallengerId() _

  def createdByDestId(userId: User.ID, max: Int = 50) = countInFor get userId flatMap { nb =>
    if (nb > 5) repo.createdByPopularDestId(max)(userId)
    else repo.createdByDestId()(userId)
  }

  def cancel(c: Challenge) =
    repo.cancel(c) >>- {
      uncacheAndNotify(c)
      Bus.publish(Event.Cancel(c.cancel), "challenge")
    }

  private def offline(c: Challenge) = (repo offline c) >>- uncacheAndNotify(c)

  private[challenge] def ping(id: Challenge.ID): Funit =
    repo statusById id flatMap {
      case Some(Status.Created) => repo setSeen id
      case Some(Status.Offline) => (repo setSeenAgain id) >> byId(id).map { _ foreach uncacheAndNotify }
      case _                    => fuccess(socketReload(id))
    }

  def decline(c: Challenge, reason: Challenge.DeclineReason) =
    repo.decline(c, reason) >>- {
      uncacheAndNotify(c)
      Bus.publish(Event.Decline(c declineWith reason), "challenge")
    }

  private val acceptQueue =
    new lila.hub.AsyncActorSequencer(maxSize = 64, timeout = 5 seconds, "challengeAccept")

  def accept(
      c: Challenge,
      user: Option[User],
      sid: Option[String],
      color: Option[chess.Color] = None
  ): Fu[Validated[String, Option[Pov]]] =
    acceptQueue {
      if (c.canceled) fuccess(Invalid("The challenge has been canceled."))
      else if (c.declined) fuccess(Invalid("The challenge has been declined."))
      else if (user.exists(_.isBot) && !Game.isBotCompatible(chess.Speed(c.clock.map(_.config))))
        fuccess(Invalid("Game incompatible with a BOT account"))
      else if (c.challengerIsOpen)
        repo.setChallenger(c.setChallenger(user, sid), color) inject Valid(none)
      else {
        joiner(c, user, color).flatMap {
          case Valid(pov) =>
            (repo accept c) >>- {
              uncacheAndNotify(c)
              Bus.publish(Event.Accept(c, user.map(_.id)), "challenge")
            } inject Valid(pov.some)
          case Invalid(err) => fuccess(Invalid(err))
        }
      }
    }

  def offerRematchForGame(game: Game, user: User): Fu[Boolean] =
    challengeMaker.makeRematchOf(game, user) flatMap {
      _ ?? { challenge =>
        create(challenge) recover lila.db.recoverDuplicateKey { _ =>
          logger.warn(s"${game.id} duplicate key ${challenge.id}")
          false
        }
      }
    }

  def setDestUser(c: Challenge, u: User): Funit = {
    val challenge = c setDestUser u
    repo.update(challenge) >>- {
      uncacheAndNotify(challenge)
      Bus.publish(Event.Create(challenge), "challenge")
    }
  }

  def removeByUserId(userId: User.ID) =
    repo allWithUserId userId flatMap { cs =>
      lila.common.Future.applySequentially(cs)(remove).void
    }

  def oauthAccept(dest: User, challenge: Challenge): Fu[Validated[String, Game]] =
    joiner(challenge, dest.some, none).map(_.map(_.game))

  private def isLimitedByMaxPlaying(c: Challenge) =
    if (c.hasClock) fuFalse
    else
      c.userIds
        .map { userId =>
          gameCache.nbPlaying(userId) dmap (lila.game.Game.maxPlaying <=)
        }
        .sequenceFu
        .dmap(_ exists identity)

  private[challenge] def sweep: Funit =
    repo.realTimeUnseenSince(DateTime.now minusSeconds 20, max = 50).flatMap { cs =>
      lila.common.Future.applySequentially(cs)(offline).void
    } >>
      repo.expired(50).flatMap { cs =>
        lila.common.Future.applySequentially(cs)(remove).void
      }

  private def remove(c: Challenge) =
    repo.remove(c.id) >>- uncacheAndNotify(c)

  private def uncacheAndNotify(c: Challenge): Unit = {
    c.destUserId ?? countInFor.invalidate
    c.destUserId ?? notifyUser.apply
    c.challengerUserId ?? notifyUser.apply
    socketReload(c.id)
  }

  private def socketReload(id: Challenge.ID): Unit =
    socket.foreach(_ reload id)

  private object notifyUser {
    private val throttler = new lila.hub.EarlyMultiThrottler(logger)
    def apply(userId: User.ID): Unit = throttler(userId, 3.seconds) {
      for {
        all  <- allFor(userId)
        lang <- userRepo langOf userId map I18nLangPicker.byStrOrDefault
      } yield Bus.publish(
        SendTo(userId, lila.socket.Socket.makeMessage("challenges", jsonView(all)(lang))),
        "socketUsers"
      )
    }
  }

  // work around circular dependency
  private var socket: Option[ChallengeSocket]               = None
  private[challenge] def registerSocket(s: ChallengeSocket) = { socket = s.some }
}
