package lila.relation

import akka.actor.{ Actor, ActorSelection }
import akka.pattern.{ ask, pipe }
import play.api.libs.json.Json

import actorApi._
import lila.hub.actorApi.relation._
import lila.hub.actorApi.{ SendTo, SendTos }
import makeTimeout.short

private[relation] final class RelationActor(
    getOnlineUserIds: () => Set[String],
    getUsername: String => Fu[String],
    api: RelationApi) extends Actor {

  private val bus = context.system.lilaBus

  private var onlines = Map[ID, Username]()

  def receive = {

    case GetOnlineFriends(userId)    => onlineFriends(userId) pipeTo sender

    // triggers following reloading for this user id
    case ReloadOnlineFriends(userId) => reloadOnlineFriends(userId)

    case AllOnlineFriends(o) => {
      onlines = o
      onlineIds foreach reloadOnlineFriends
    }

    case ReloadAllOnlineFriends =>
      (getOnlineUserIds() map { id =>
        getUsername(id) map (id -> _)
      }).sequenceFu map { users => AllOnlineFriends(users.toMap) } pipeTo self

    case NotifyMovement => {
      val prevIds = onlineIds
      val curIds = getOnlineUserIds()
      val leaveIds = (prevIds diff curIds).toList
      val enterIds = (curIds diff prevIds).toList

      val leaves: List[User] = leaveIds map { id =>
        onlines get id map { id -> _ }
      } flatten

      val enters: Fu[List[User]] =
        (enterIds map { id => getUsername(id) map { id -> _ } }).sequenceFu

      enters map { Movement(leaves, _) } pipeTo self
    }

    case Movement(leaves, enters) => {

      onlines = onlines -- leaves.map(_._1) ++ enters

      notifyFollowers(enters, "following_enters")
      notifyFollowers(leaves, "following_leaves")
    }
  }

  private def onlineIds: Set[ID] = onlines.keySet

  private def onlineFriends(userId: String): Fu[OnlineFriends] = for {
    ids ← api following userId
    usernames ← ((ids intersect onlineIds).toList map getUsername).sequenceFu
  } yield OnlineFriends(usernames)

  private def reloadOnlineFriends(userId: String) {
    onlineFriends(userId) foreach {
      case OnlineFriends(usernames) =>
        bus.publish(SendTo(userId, "following_onlines", usernames), 'users)
    }
  }

  private def notifyFollowers(users: List[User], message: String) {
    users foreach {
      case (id, name) => api followers id foreach { ids =>
        val notify = ids filter onlines.contains
        if (notify.nonEmpty) bus.publish(SendTos(notify.toSet, message, name), 'users)
      }
    }
  }
}
