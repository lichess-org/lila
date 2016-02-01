package lila.challenge

import akka.actor.ActorSelection
import scala.concurrent.duration._

import lila.hub.actorApi.SendTo
import lila.memo.{ MixedCache, AsyncCache }
import lila.user.{ User, UserRepo }

final class ChallengeApi(
    repo: ChallengeRepo,
    jsonView: JsonView,
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

  def createdByDestIds = repo createdByDestIds _

  def accept(c: Challenge) = (repo accept c) >> {
    uncacheAndNotify(c)
  }

  def cancel(c: Challenge) = (repo cancel c) >> {
    uncacheAndNotify(c)
  }

  def abandon(c: Challenge) = (repo abandon c) >> {
    uncacheAndNotify(c)
  }

  def decline(c: Challenge) = (repo decline c) >> {
    uncacheAndNotify(c)
  }

  private def uncacheAndNotify(c: Challenge) = {
    (c.destUserId ?? countInFor.remove) >>-
      (c.destUserId ?? notify) >>-
      (c.challengerUserId ?? notify)
  }

  private def notify(userId: User.ID) {
    allFor(userId) foreach { all =>
      userRegister ! SendTo(userId, lila.socket.Socket.makeMessage("challenges", jsonView(all)))
    }
  }
}
