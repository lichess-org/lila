package lila.challenge

import akka.actor._
import scala.concurrent.duration._

import lila.game.Game
import lila.hub.actorApi.map.Tell
import lila.hub.actorApi.SendTo
import lila.memo.{ MixedCache, AsyncCache }
import lila.user.{ User, UserRepo }

final class ChallengeApi(
    repo: ChallengeRepo,
    joiner: Joiner,
    jsonView: JsonView,
    socketHub: ActorRef,
    userRegister: ActorSelection) {

  import Challenge._

  def allFor(userId: User.ID): Fu[AllChallenges] =
    createdByDestId(userId) zip createdByChallengerId(userId) map (AllChallenges.apply _).tupled

  def create(c: Challenge): Funit = (repo insert c) >> {
    uncacheAndNotify(c)
  }

  def byId = repo byId _

  val countInFor = AsyncCache(repo.countCreatedByDestId, maxCapacity = 20000)

  def createdByChallengerId = repo createdByChallengerId _

  def createdByDestId = repo createdByDestId _

  def cancel(c: Challenge) = (repo cancel c) >> uncacheAndNotify(c)

  def offline(c: Challenge) = (repo offline c) >> uncacheAndNotify(c)

  def ping(id: Challenge.ID): Funit = repo statusById id flatMap {
    case Some(Status.Created) => repo setSeen id
    case Some(Status.Offline) => (repo setSeenAgain id) >> byId(id).flatMap { _ ?? uncacheAndNotify }
    case _                    => fuccess(socketReload(id))
  }

  def decline(c: Challenge) = (repo decline c) >> uncacheAndNotify(c)

  def accept(c: Challenge, user: Option[User]): Fu[Game] =
    joiner(c, user).flatMap { game =>
      (repo accept c) >> uncacheAndNotify(c) inject game
    }

  private def uncacheAndNotify(c: Challenge) = {
    (c.destUserId ?? countInFor.remove) >>-
      (c.destUserId ?? notify) >>-
      (c.challengerUserId ?? notify) >>-
      socketReload(c.id)
  }

  private def socketReload(id: Challenge.ID) {
    socketHub ! Tell(id, Socket.Reload)
  }

  private def notify(userId: User.ID) {
    allFor(userId) foreach { all =>
      userRegister ! SendTo(userId, lila.socket.Socket.makeMessage("challenges", jsonView(all)))
    }
  }
}
