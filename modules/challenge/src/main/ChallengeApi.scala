package lila.challenge

import akka.actor._
import org.joda.time.DateTime
import scala.concurrent.duration._

import lila.game.{ Game, Pov, GameRepo }
import lila.hub.actorApi.map.Tell
import lila.hub.actorApi.SendTo
import lila.user.{ User, UserRepo }

final class ChallengeApi(
    repo: ChallengeRepo,
    joiner: Joiner,
    jsonView: JsonView,
    gameCache: lila.game.Cached,
    maxPlaying: Int,
    socketHub: ActorRef,
    userRegister: ActorSelection,
    asyncCache: lila.memo.AsyncCache.Builder,
    lilaBus: lila.common.Bus
) {

  import Challenge._

  def allFor(userId: User.ID): Fu[AllChallenges] =
    createdByDestId(userId) zip createdByChallengerId(userId) map (AllChallenges.apply _).tupled

  // returns boolean success
  def create(c: Challenge): Fu[Boolean] = isLimitedByMaxPlaying(c) flatMap {
    case true => fuFalse
    case false => {
      repo like c flatMap { _ ?? repo.cancel }
    } >> (repo insert c) >>- {
      uncacheAndNotify(c)
      lilaBus.publish(Event.Create(c), 'challenge)
    } inject true
  }

  def byId = repo byId _

  def activeByIdFor(id: Challenge.ID, dest: User) = repo.byIdFor(id, dest).map(_.filter(_.active))

  def onlineByIdFor(id: Challenge.ID, dest: User) = repo.byIdFor(id, dest).map(_.filter(_.online))

  val countInFor = asyncCache.clearable(
    name = "challenge.countInFor",
    f = repo.countCreatedByDestId,
    expireAfter = _.ExpireAfterAccess(20 minutes)
  )

  def createdByChallengerId = repo createdByChallengerId _

  def createdByDestId = repo createdByDestId _

  def cancel(c: Challenge) = (repo cancel c) >>- uncacheAndNotify(c)

  private def offline(c: Challenge) = (repo offline c) >>- uncacheAndNotify(c)

  private[challenge] def ping(id: Challenge.ID): Funit = repo statusById id flatMap {
    case Some(Status.Created) => repo setSeen id
    case Some(Status.Offline) => (repo setSeenAgain id) >> byId(id).map { _ foreach uncacheAndNotify }
    case _ => fuccess(socketReload(id))
  }

  def decline(c: Challenge) = (repo decline c) >>- uncacheAndNotify(c)

  def accept(c: Challenge, user: Option[User]): Fu[Option[Pov]] =
    joiner(c, user).flatMap {
      case None => fuccess(None)
      case Some(pov) => (repo accept c) >>- {
        uncacheAndNotify(c)
        lilaBus.publish(Event.Accept(c, user.map(_.id)), 'challenge)
      } inject pov.some
    }

  def sendRematchOf(game: Game, user: User): Fu[Boolean] =
    ChallengeMaker.makeRematchOf(game, user) flatMap { _ ?? create }

  def setDestUser(c: Challenge, u: User): Funit = {
    val challenge = c setDestUser u
    repo.update(challenge) >>- {
      uncacheAndNotify(challenge)
      lilaBus.publish(Event.Create(challenge), 'challenge)
    }
  }

  def removeByUserId(userId: User.ID) = repo allWithUserId userId flatMap { cs =>
    lila.common.Future.applySequentially(cs)(remove).void
  }

  private def isLimitedByMaxPlaying(c: Challenge) =
    if (c.hasClock) fuFalse
    else c.userIds.map { userId =>
      gameCache.nbPlaying(userId) map (maxPlaying <=)
    }.sequenceFu.map(_ exists identity)

  private[challenge] def sweep: Funit =
    repo.realTimeUnseenSince(DateTime.now minusSeconds 10, max = 50).flatMap { cs =>
      lila.common.Future.applySequentially(cs)(offline).void
    } >>
      repo.expired(50).flatMap { cs =>
        lila.common.Future.applySequentially(cs)(remove).void
      }

  private def remove(c: Challenge) =
    repo.remove(c.id) >>- uncacheAndNotify(c)

  private def uncacheAndNotify(c: Challenge): Unit = {
    c.destUserId ?? countInFor.invalidate
    c.destUserId ?? notify
    c.challengerUserId ?? notify
    socketReload(c.id)
  }

  private def socketReload(id: Challenge.ID): Unit =
    socketHub ! Tell(id, Socket.Reload)

  private def notify(userId: User.ID): Funit = for {
    all <- allFor(userId)
    lang <- UserRepo langOf userId map {
      _ flatMap lila.i18n.I18nLangPicker.byStr getOrElse lila.i18n.defaultLang
    }
  } yield {
    userRegister ! SendTo(userId, lila.socket.Socket.makeMessage("challenges", jsonView(all, lang)))
  }
}
