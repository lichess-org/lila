package lila.relation

import akka.actor.{ Actor, ActorSelection }
import akka.pattern.{ ask, pipe }
import play.api.libs.json.Json

import actorApi._
import lila.common.LightUser
import lila.hub.actorApi.relation._
import lila.hub.actorApi.{ SendTo, SendTos }
import makeTimeout.short

private[relation] final class RelationActor(
    getOnlineUserIds: () => Set[String],
    lightUser: String => Option[LightUser],
    api: RelationApi) extends Actor {

  private val bus = context.system.lilaBus

  private var onlines = Map[ID, LightUser]()

  def receive = {

    case GetOnlineFriends(userId)    => onlineFriends(userId) pipeTo sender

    // triggers following reloading for this user id
    case ReloadOnlineFriends(userId) => reloadOnlineFriends(userId)

    case AllOnlineFriends(o) =>
      onlines = o
      onlineIds foreach reloadOnlineFriends

    case ReloadAllOnlineFriends => self ! AllOnlineFriends(
      getOnlineUserIds().map { id => lightUser(id) map (id -> _) }.flatten.toMap
    )

    case NotifyMovement =>
      val prevIds = onlineIds
      val curIds = getOnlineUserIds()
      val leaveIds = (prevIds diff curIds).toList
      val enterIds = (curIds diff prevIds).toList
      val leaves = leaveIds.map(onlines.get).flatten
      val enters = enterIds.map(onlines.get).flatten
      self ! Movement(leaves, enters)

    case Movement(leaves, enters) =>
      onlines = onlines -- leaves.map(_.id) ++ enters.map(e => e.id -> e)
      notifyFollowers(enters, "following_enters")
      notifyFollowers(leaves, "following_leaves")
  }

  private def onlineIds: Set[ID] = onlines.keySet

  private def onlineFriends(userId: String): Fu[OnlineFriends] =
    api following userId map { ids =>
      OnlineFriends((ids intersect onlineIds).map(lightUser).flatten.toList)
    }

  private def reloadOnlineFriends(userId: String) {
    onlineFriends(userId) foreach {
      case OnlineFriends(users) =>
        bus.publish(SendTo(userId, "following_onlines", users.map(_.titleName)), 'users)
    }
  }

  private def notifyFollowers(users: List[LightUser], message: String) {
    users foreach { user =>
      api followers user.id foreach { ids =>
        val notify = ids filter onlines.contains
        if (notify.nonEmpty) bus.publish(SendTos(notify.toSet, message, user.titleName), 'users)
      }
    }
  }
}
