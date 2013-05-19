package lila.friend

import actorApi._
import lila.hub.actorApi.SendTos
import lila.hub.actorApi.friend.GetFriends

import akka.actor.Actor
import akka.pattern.{ ask, pipe }

private[friend] final class FriendActor(
    socketHub: lila.hub.ActorLazyRef,
    getOnlineUserIds: () ⇒ Set[String],
    getUsername: String ⇒ Fu[String],
    getFriendIds: String ⇒ Fu[List[String]]) extends Actor {

  def receive = {

    // called only once by the websocket when it connects
    case GetFriends(userId) ⇒ getFriendIds(userId) flatMap { friendIds ⇒
      ((friendIds.toSet intersect onlineIds).toList map getUsername).sequence
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
        (enterIds map { id ⇒ getUsername(id) map { id -> _ } }).sequence
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
