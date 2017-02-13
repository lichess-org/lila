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
    getOnlineUserIds: () => Set[String],
    lightUser: LightUser.Getter,
    lightUserSync: LightUser.GetterSync,
    api: RelationApi,
    onlinePlayings: ExpireSetMemo,
    onlineStudying: OnlineStudyingMemo,
    onlineStudyingAll: OnlineStudyingMemo) extends Actor {

  private val bus = context.system.lilaBus

  private var onlines = Map[ID, LightUser]()

  override def preStart(): Unit = {
    context.system.lilaBus.subscribe(self, 'startGame)
    context.system.lilaBus.subscribe(self, 'finishGame)
    context.system.lilaBus.subscribe(self, 'study)
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
      val prevIds = onlineIds
      val curIds = getOnlineUserIds()
      val leaveIds = (prevIds diff curIds).toList
      val enterIds = (curIds diff prevIds).toList
      for {
        leaves <- leaveIds.map(lightUser).sequenceFu.map(_.flatten)
        enters <- enterIds.map(lightUser).sequenceFu.map(_.flatten)
      } self ! NotifyMovement(leaves, enters)

    case NotifyMovement(leaves, enters) =>
      onlines = onlines -- leaves.map(_.id) ++ enters.map(e => e.id -> e)
      val friendsEntering = enters.map(makeFriendEntering)
      notifyFollowersFriendEnters(friendsEntering)
      notifyFollowersFriendLeaves(leaves)

    case lila.game.actorApi.FinishGame(game, _, _) if game.hasClock =>
      val usersPlaying = game.userIds
      onlinePlayings removeAll usersPlaying
      notifyFollowersGameStateChanged(usersPlaying, "following_stopped_playing")

    case lila.game.actorApi.StartGame(game) if game.hasClock =>
      val usersPlaying = game.userIds
      onlinePlayings putAll usersPlaying
      notifyFollowersGameStateChanged(usersPlaying, "following_playing")

    case lila.hub.actorApi.study.StudyJoin(userId, studyId, contributor, public) =>
      onlineStudyingAll.put(userId, studyId)
      if (contributor && public) {
        val wasAlreadyInStudy = onlineStudying.get(userId).isDefined
        onlineStudying.put(userId, studyId)
        if (!wasAlreadyInStudy) notifyFollowersFriendInStudyStateChanged(userId, studyId, "following_joined_study")
      }
 
    case lila.hub.actorApi.study.StudyQuit(userId, studyId, contributor, public) =>
      onlineStudyingAll remove userId
      if (contributor && public) {
        onlineStudying remove userId
        notifyFollowersFriendInStudyStateChanged(userId, studyId, "following_left_study")
      }

    case lila.hub.actorApi.study.StudyBecamePrivate(studyId, contributors) =>
      val contributorsInStudy = contributors.filter(onlineStudying.get(_).isDefined)
      for (c <- contributorsInStudy) {
        onlineStudying remove c
        notifyFollowersFriendInStudyStateChanged(c, studyId, "following_left_study")
      }

    case lila.hub.actorApi.study.StudyBecamePublic(studyId, contributors) =>
      val contributorsInStudy = contributors.filter(onlineStudyingAll.get(_).isDefined)
      for (c <- contributorsInStudy) {
        onlineStudying.put(c, studyId)
        notifyFollowersFriendInStudyStateChanged(c, studyId, "following_joined_study")
      }

    case lila.hub.actorApi.study.StudyMemberGotWriteAccess(userId, studyId, public) if public =>
      if (onlineStudyingAll.get(userId).isDefined) {
        onlineStudying.put(userId, studyId)
        notifyFollowersFriendInStudyStateChanged(userId, studyId, "following_joined_study")
      }

    case lila.hub.actorApi.study.StudyMemberLostWriteAccess(userId, studyId, public) if public =>
      if (onlineStudying.get(userId).isDefined) {
        onlineStudying remove userId
        notifyFollowersFriendInStudyStateChanged(userId, studyId, "following_left_study")
      }
  }

  private def makeFriendEntering(enters: LightUser) = {
    FriendEntering(enters, onlinePlayings.get(enters.id), onlineStudying.get(enters.id).isDefined)
  }

  private def onlineIds: Set[ID] = onlines.keySet

  private def onlineFriends(userId: String): Fu[OnlineFriends] =
    api fetchFollowing userId map { ids =>
      val friends = ids.flatMap(onlines.get).toList
      val friendsPlaying = filterFriendsPlaying(friends)
      val friendsStudying = filterFriendsStudying(friends)
      OnlineFriends(friends, friendsPlaying, friendsStudying)
    }

  private def filterFriendsPlaying(friends: List[LightUser]): Set[String] = {
    friends.filter(p => onlinePlayings.get(p.id)).map(_.id).toSet
  }

  private def filterFriendsStudying(friends: List[LightUser]): Set[String] = {
    friends.filter(p => onlineStudying.get(p.id).isDefined).map(_.id).toSet
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

  private def notifyFollowersFriendInStudyStateChanged(userId: String, studyId: String, message: String) =
    api fetchFollowers userId map (_ filter onlines.contains) foreach { ids =>
      if (ids.nonEmpty) bus.publish(SendTos(ids.toSet, message, userId), 'users)
    }
}
