package lila.friend

import lila.hub.actorApi.SendTos

import akka.actor.Actor

private[friend] final class OnlineWatcher(
    socketHub: lila.hub.ActorLazyRef,
    getOnlineUserIds: () ⇒ Set[String],
    getUsername: String ⇒ Fu[String],
    getFriendIds: String ⇒ Fu[List[String]]) extends Actor {

  private type ID = String
  private type Username = String
  private type User = (ID, Username)

  // id -> username
  private var onlines = Map[ID, Username]()

  private def onlineIds: Set[ID] = onlines.keySet

  def receive = {

    case true ⇒ {
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
      notifyFriends(leaves, "friend_enters")
    }
  }

  private def notifyFriends(users: List[User], message: String) {
    users foreach {
      case (id, name) ⇒ getFriendIds(id) foreach { ids ⇒
        val notify = ids filter onlines.contains
        if (notify.nonEmpty) socketHub ! SendTos(notify.toSet, message, name)
      }
    }
  }
}
