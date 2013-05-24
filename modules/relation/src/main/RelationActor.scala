package lila.relation

import akka.actor.Actor
import akka.pattern.{ ask, pipe }

import actorApi._
import lila.hub.actorApi.relation._
import lila.hub.actorApi.SendTos
import lila.hub.actorApi.timeline.{ MakeEntry, ShareEntry }
import lila.hub.ActorLazyRef

private[relation] final class RelationActor(
    socketHub: ActorLazyRef,
    getOnlineUserIds: () ⇒ Set[String],
    getUsername: String ⇒ Fu[String],
    getFriendIds: String ⇒ Fu[Set[String]],
    timelinePush: ActorLazyRef) extends Actor {

  def receive = {

    case ShareEntry(userId, data) ⇒ getFriendIds(userId) map { ids ⇒
      MakeEntry(ids.toList, data)
    } pipeTo timelinePush.ref

    // rarely called
    // return a list of usernames, followers, following and online
    case GetFriends(userId) ⇒ getFriendIds(userId) flatMap { ids ⇒
      ((ids intersect onlineIds).toList map getUsername).sequenceFu
    } pipeTo sender

    case NotifyMovement ⇒ {
      val prevIds = onlineIds
      val curIds = getOnlineUserIds()
      val leaveIds = (prevIds diff curIds).toList
      val enterIds = (curIds diff prevIds).toList

      val leaves: List[User] = leaveIds map { id ⇒
        onlines get id map { id -> _ }
      } flatten

      val enters: List[User] = {
        (enterIds map { id ⇒ getUsername(id) map { id -> _ } }).sequenceFu
      }.await

      onlines = onlines -- leaveIds ++ enters

      notifyFriends(enters, "friend_enters")
      notifyFriends(leaves, "friend_leaves")
    }
  }

  private type ID = String
  private type Username = String
  private type User = (ID, Username)

  private var onlines = Map[ID, Username]()
  private def onlineIds: Set[ID] = onlines.keySet

  private def notifyFriends(users: List[User], message: String) {
    users foreach {
      case (id, name) ⇒ getFriendIds(id) foreach { ids ⇒
        val notify = ids filter onlines.contains
        if (notify.nonEmpty) socketHub ! SendTos(notify.toSet, message, name)
      }
    }
  }
}
