package lila.relation

import akka.actor.Actor
import akka.pattern.pipe
import lila.memo.ExpireSetMemo
import scala.concurrent.duration._

import actorApi._
import lila.common.LightUser
import lila.hub.actorApi.relation._
import lila.hub.actorApi.{ SendTo, SendTos }

import play.api.libs.json.Json

private[relation] final class RelationActor(
    lightUser: LightUser.GetterSync,
    api: RelationApi,
    onlineUserIds: ExpireSetMemo,
    onlinePlayings: ExpireSetMemo,
    onlineStudying: OnlineStudyingCache,
    onlineStudyingAll: OnlineStudyingCache
) extends Actor {

  private val bus = context.system.lilaBus

  private var previousOnlineIds = Set.empty[ID]

  override def preStart(): Unit = {
    context.system.lilaBus.subscribe(self, 'startGame, 'finishGame, 'study)
  }

  override def postStop(): Unit = {
    super.postStop()
    context.system.lilaBus.unsubscribe(self)
  }

  def receive = {

    case GetOnlineFriends(userId) => onlineFriends(userId) pipeTo sender

    // triggers following reloading for this user id
    case ReloadOnlineFriends(userId) => onlineFriends(userId) foreach { res =>
      bus.publish(SendTo(userId, JsonView.writeOnlineFriends(res)), 'users)
    }

    case ComputeMovement =>
      val curIds = onlineUserIds.keySet
      val leaveIds = (previousOnlineIds diff curIds).toList
      val enterIds = (curIds diff previousOnlineIds).toList
      val leaveUsers = leaveIds flatMap { lightUser(_) }
      val enterUsers = enterIds flatMap { lightUser(_) }

      val friendsEntering = enterUsers map { u =>
        FriendEntering(u, onlinePlayings get u.id, onlineStudying.getIfPresent(u.id).isDefined)
      }

      notifyFollowersFriendEnters(friendsEntering)
      notifyFollowersFriendLeaves(leaveUsers)
      previousOnlineIds = curIds

    case lila.game.actorApi.FinishGame(game, _, _) if game.hasClock =>
      val usersPlaying = game.userIds
      onlinePlayings removeAll usersPlaying
      notifyFollowersGameStateChanged(usersPlaying, "following_stopped_playing")

    case lila.game.actorApi.StartGame(game) if game.hasClock =>
      val usersPlaying = game.userIds
      onlinePlayings putAll usersPlaying
      notifyFollowersGameStateChanged(usersPlaying, "following_playing")

    case lila.hub.actorApi.study.StudyDoor(userId, studyId, contributor, public, true) =>
      onlineStudyingAll.put(userId, studyId)
      if (contributor && public) {
        val wasAlreadyInStudy = onlineStudying.getIfPresent(userId).isDefined
        onlineStudying.put(userId, studyId)
        if (!wasAlreadyInStudy) notifyFollowersFriendInStudyStateChanged(userId, studyId, "following_joined_study")
      }

    case lila.hub.actorApi.study.StudyDoor(userId, studyId, contributor, public, false) =>
      onlineStudyingAll invalidate userId
      if (contributor && public) {
        onlineStudying invalidate userId
        notifyFollowersFriendInStudyStateChanged(userId, studyId, "following_left_study")
      }

    case lila.hub.actorApi.study.StudyBecamePrivate(studyId, contributors) =>
      val found = onlineStudying.getAllPresent(contributors).filter(_._2 == studyId)
      val contributorsInStudy = contributors filter found.contains
      for (c <- contributorsInStudy) {
        onlineStudying invalidate c
        notifyFollowersFriendInStudyStateChanged(c, studyId, "following_left_study")
      }

    case lila.hub.actorApi.study.StudyBecamePublic(studyId, contributors) =>
      val found = onlineStudyingAll.getAllPresent(contributors).filter(_._2 == studyId)
      val contributorsInStudy = contributors filter found.contains
      for (c <- contributorsInStudy) {
        onlineStudying.put(c, studyId)
        notifyFollowersFriendInStudyStateChanged(c, studyId, "following_joined_study")
      }

    case lila.hub.actorApi.study.StudyMemberGotWriteAccess(userId, studyId) =>
      if (onlineStudyingAll.getIfPresent(userId) has studyId) {
        onlineStudying.put(userId, studyId)
        notifyFollowersFriendInStudyStateChanged(userId, studyId, "following_joined_study")
      }

    case lila.hub.actorApi.study.StudyMemberLostWriteAccess(userId, studyId) =>
      if (onlineStudying.getIfPresent(userId) has studyId) {
        onlineStudying invalidate userId
        notifyFollowersFriendInStudyStateChanged(userId, studyId, "following_left_study")
      }
  }

  private def onlineFriends(userId: ID): Fu[OnlineFriends] =
    api fetchFollowing userId map onlineUserIds.intersect map { friends =>
      OnlineFriends(
        users = friends.flatMap { lightUser(_) },
        playing = filterFriendsPlaying(friends),
        studying = filterFriendsStudying(friends)
      )
    }

  private def filterFriendsPlaying(friendIds: Set[ID]): Set[ID] =
    onlinePlayings intersect friendIds

  private def filterFriendsStudying(friendIds: Set[ID]): Set[ID] = {
    val found = onlineStudying.getAllPresent(friendIds)
    friendIds filter found.contains
  }

  private def notifyFollowersFriendEnters(friendsEntering: List[FriendEntering]) =
    friendsEntering foreach { entering =>
      api fetchFollowers entering.user.id map onlineUserIds.intersect foreach { ids =>
        if (ids.nonEmpty) bus.publish(SendTos(ids.toSet, JsonView.writeFriendEntering(entering)), 'users)
      }
    }

  private def notifyFollowersFriendLeaves(friendsLeaving: List[LightUser]) =
    friendsLeaving foreach { leaving =>
      api fetchFollowers leaving.id map onlineUserIds.intersect foreach { ids =>
        if (ids.nonEmpty) bus.publish(SendTos(ids.toSet, "following_leaves", leaving.titleName), 'users)
      }
    }

  private def notifyFollowersGameStateChanged(userIds: Traversable[ID], message: String) =
    userIds foreach { userId =>
      api fetchFollowers userId map onlineUserIds.intersect foreach { ids =>
        if (ids.nonEmpty) bus.publish(SendTos(ids.toSet, message, userId), 'users)
      }
    }

  private def notifyFollowersFriendInStudyStateChanged(userId: ID, studyId: String, message: String) =
    api fetchFollowers userId map onlineUserIds.intersect foreach { ids =>
      if (ids.nonEmpty) bus.publish(SendTos(ids.toSet, message, userId), 'users)
    }
}
