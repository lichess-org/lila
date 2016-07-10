package lila.relation

import akka.actor.{ Actor, ActorSelection }
import akka.pattern.{ ask, pipe }
import lila.game.Game
import lila.memo.ExpireSetMemo
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

  private val onlinePlayings = new ExpireSetMemo(1 hour)

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
      case onlineFriends =>
        bus.publish(SendTo(userId, JsonView.writeOnlineFriends(onlineFriends)), 'users)
    }

    case NotifyMovement =>
      val prevIds = onlineIds
      val curIds = getOnlineUserIds()
      val leaveIds = (prevIds diff curIds).toList
      val enterIds = (curIds diff prevIds).toList
      val leaves = leaveIds.flatMap(i => lightUser(i))
      val enters = enterIds.flatMap(i => lightUser(i))
      onlines = onlines -- leaveIds ++ enters.map(e => e.id -> e)

      val friendsEntering = enters.map(makeFriendEntering)
      notifyFollowersFriendEnters(friendsEntering)
      notifyFollowersFriendLeaves(leaves)

    case lila.game.actorApi.FinishGame(game, whiteUserOption, blackUserOption) if game.hasClock =>
      val usersPlaying = game.userIds
      usersPlaying.foreach(onlinePlayings.remove)
      notifyFollowersGameStateChanged(usersPlaying, "following_stopped_playing")

    case lila.game.actorApi.StartGame(game) if game.hasClock =>
      val usersPlaying = game.userIds
      onlinePlayings.putAll(usersPlaying)
      notifyFollowersGameStateChanged(usersPlaying, "following_playing")
  }

  private def makeFriendEntering(enters: LightUser) = {
    FriendEntering(enters, onlinePlayings.get(enters.id))
  }

  private def onlineIds: Set[ID] = onlines.keySet

  private def onlineFriends(userId: String): Fu[OnlineFriends] =
    api fetchFollowing userId map { ids =>
      val friends = ids.flatMap(onlines.get).toList
      val friendsPlaying = filterFriendsPlaying(friends)
      OnlineFriends(friends, friendsPlaying)
    }

  private def filterFriendsPlaying(friends: List[LightUser]): Set[String] = {
    friends.filter(p => onlinePlayings.get(p.id)).map(_.id).toSet
  }

  private def notifyFollowersFriendEnters(friendsEntering: List[FriendEntering]) =
    friendsEntering foreach { entering =>
      api fetchFollowers entering.user.id map (_ filter onlines.contains) foreach { ids =>
        if (ids.nonEmpty) bus.publish(SendTos(ids.toSet, JsonView.writeFriendEntering(entering)), 'users)
      }
    }

  private def notifyFollowersFriendLeaves(friendsLeaving: List[LightUser]) =
    friendsLeaving foreach { leaving =>
      api fetchFollowers leaving.id map (_ filter onlines.contains) foreach { ids =>
        if (ids.nonEmpty) bus.publish(SendTos(ids.toSet, "following_leaves", leaving.titleName), 'users)
      }
    }

  private def notifyFollowersGameStateChanged(userIds: Traversable[String], message: String) =
    userIds foreach { userId =>
      api fetchFollowers userId map (_ filter onlines.contains) foreach { ids =>
        if (ids.nonEmpty) bus.publish(SendTos(ids.toSet, message, userId), 'users)
      }
    }
}
