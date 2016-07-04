package lila.relation

import akka.actor.{ Actor, ActorSelection }
import akka.pattern.{ ask, pipe }
import lila.game.Game
import play.api.libs.json.Json
import scala.concurrent.duration._

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

  private var onlinePlayings = Set[ID]()

  override def preStart(): Unit = {
    context.system.lilaBus.subscribe(self, 'startGame)
    context.system.lilaBus.subscribe(self, 'finishGame)
  }

  override def postStop() : Unit = {
    super.postStop()
    context.system.lilaBus.unsubscribe(self)
  }

  def receive = {

    case GetOnlineFriends(userId) => onlineFriends(userId) pipeTo sender

    // triggers following reloading for this user id
    case ReloadOnlineFriends(userId) => onlineFriends(userId) foreach {
      case OnlineFriends(users, friendsPlaying) =>
        bus.publish(SendTo(userId, "following_onlines", users.map(_.titleName)), 'users)
        bus.publish(SendTo(userId, "following_playings", friendsPlaying), 'users)
    }

    case NotifyMovement =>
      val prevIds = onlineIds
      val curIds = getOnlineUserIds()
      val leaveIds = (prevIds diff curIds).toList
      val enterIds = (curIds diff prevIds).toList
      val leaves = leaveIds.flatMap(i => lightUser(i))
      val enters = enterIds.flatMap(i => lightUser(i))
      onlines = onlines -- leaveIds ++ enters.map(e => e.id -> e)
      notifyFollowersByTitle(enters, "following_enters")
      notifyFollowersByTitle(leaves, "following_leaves")

    case lila.game.actorApi.FinishGame(game, whiteUserOption, blackUserOption) =>
      onlinePlayings = onlinePlayings -- game.userIds
      val usersPlaying = game.userIds
      notifyFollowersById(usersPlaying, "following_stopped_playing")

    case msg: lila.game.actorApi.StartGame =>
      val usersPlaying = msg.game.userIds
      onlinePlayings = onlinePlayings ++ usersPlaying
      notifyFollowersById(usersPlaying, "following_playing")
  }

  private def onlineIds: Set[ID] = onlines.keySet

  private def onlineFriends(userId: String): Fu[OnlineFriends] =
    api fetchFollowing userId map { ids =>
      val friends = ids.flatMap(onlines.get).toList
      val friendsPlaying = getFriendsPlaying(friends)
      OnlineFriends(friends, friendsPlaying)
    }

  private def getFriendsPlaying(friends: List[LightUser]): Set[String] = {
    friends.filter(p => onlinePlayings.contains(p.id)).map(_.id).toSet
  }

  private def notifyFollowers(users: List[LightUser], message: String) {
    users foreach { user =>
      api fetchFollowers user.id map (_ filter onlines.contains) foreach { ids =>
        if (ids.nonEmpty) bus.publish(SendTos(ids.toSet, message, user.titleName), 'users)
      }
    }
  }

  private def notifyFollowersByTitle(users: List[LightUser], message: String) =
    users foreach { user =>
      api fetchFollowers user.id map (_ filter onlines.contains) foreach { ids =>
        if (ids.nonEmpty) bus.publish(SendTos(ids.toSet, message, user.titleName), 'users)
      }
    }

  private def notifyFollowersById(userIds: List[String], message: String) =
    userIds foreach { userId =>
      api fetchFollowers userId map (_ filter onlines.contains) foreach { ids =>
        if (ids.nonEmpty) bus.publish(SendTos(ids.toSet, message, userId), 'users)
      }
    }
}
