package lila.relation

import akka.actor.Actor
import akka.pattern.{ ask, pipe }
import play.api.libs.json.Json

import actorApi._
import lila.hub.actorApi.relation._
import lila.hub.actorApi.{ SendTo, SendTos }
import lila.hub.ActorLazyRef
import makeTimeout.short

private[relation] final class RelationActor(
    socketHub: ActorLazyRef,
    getOnlineUserIds: () ⇒ Set[String],
    getUsername: String ⇒ Fu[String],
    api: RelationApi) extends Actor {

  private type ID = String
  private type Username = String
  private type User = (ID, Username)

  def receive = {

    case GetOnlineFriends(userId)    ⇒ onlineFriends(userId) pipeTo sender

    // triggers following reloading for this user id
    case ReloadOnlineFriends(userId) ⇒ reloadOnlineFriends(userId)

    case ReloadAllOnlineFriends ⇒ {
      (getOnlineUserIds() map { id ⇒
        getUsername(id) map (id -> _)
      }).sequenceFu map { users ⇒
        onlines = users.toMap
        onlineIds foreach reloadOnlineFriends
      }
    }.await

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

      notifyFollowers(enters, "following_enters")
      notifyFollowers(leaves, "following_leaves")
    }
  }

  private var onlines = Map[ID, Username]()
  private def onlineIds: Set[ID] = onlines.keySet

  private def onlineFriends(userId: String): Fu[OnlineFriends] = for {
    ids ← api.following(userId)
    usernames ← ((ids intersect onlineIds).toList map getUsername).sequenceFu
  } yield OnlineFriends(usernames, ids.size)

  private def reloadOnlineFriends(userId: String) {
    onlineFriends(userId) map {
      case OnlineFriends(usernames, nb) ⇒
        SendTo(userId, "following_onlines", Json.obj(
          "us" -> usernames,
          "nb" -> nb
        ))
    } pipeToSelection socketHub.selection
  }

  private def notifyFollowers(users: List[User], message: String) {
    users foreach {
      case (id, name) ⇒ api.followers(id) foreach { ids ⇒
        val notify = ids filter onlines.contains
        if (notify.nonEmpty) socketHub ! SendTos(notify.toSet, message, name)
      }
    }
  }
}
