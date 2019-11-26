package lila.relation

import akka.actor.Actor
import scala.collection.breakOut

import actorApi._
import lila.common.{ Bus, LightUser }
import lila.hub.actorApi.relation._
import lila.hub.actorApi.socket.{ SendTo, SendTos }
import lila.user.User

private[relation] final class RelationActor(
    lightUser: LightUser.GetterSync,
    api: RelationApi,
    online: OnlineDoing
) extends Actor {

  private var previousOnlineIds = Set.empty[ID]

  private val subs = List('startGame, 'finishGame, 'study, 'reloadOnlineFriends)

  override def preStart(): Unit = {
    Bus.subscribe(self, subs)
  }

  override def postStop(): Unit = {
    super.postStop()
    Bus.unsubscribe(self, subs)
  }

  def receive = {

    case ComputeMovement =>
      val curIds = online.userIds()
      val leaveUsers: List[LightUser] = (previousOnlineIds diff curIds).flatMap { lightUser(_) }(breakOut)
      val enterUsers: List[LightUser] = (curIds diff previousOnlineIds).flatMap { lightUser(_) }(breakOut)

      val friendsEntering = enterUsers map { u =>
        FriendEntering(u, online.playing get u.id, online isStudying u.id)
      }

      notifyFollowersFriendEnters(friendsEntering, curIds)
      notifyFollowersFriendLeaves(leaveUsers, curIds)
      previousOnlineIds = curIds

    // triggers following reloading for this user id
    case ReloadOnlineFriends(userId) => online friendsOf userId foreach { res =>
      // the mobile app requests this on every WS connection
      // we can skip it if empty
      if (!res.isEmpty) Bus.publish(SendTo(userId, JsonView writeOnlineFriends res), 'socketUsers)
    }

    case lila.game.actorApi.FinishGame(game, _, _) if game.hasClock =>
      val usersPlaying = game.userIds
      online.playing removeAll usersPlaying
      notifyFollowersGameStateChanged(usersPlaying, "following_stopped_playing")

    case lila.game.actorApi.StartGame(game) if game.hasClock =>
      val usersPlaying = game.userIds
      online.playing putAll usersPlaying
      notifyFollowersGameStateChanged(usersPlaying, "following_playing")

    case lila.hub.actorApi.study.StudyDoor(userId, studyId, contributor, public, true) =>
      online.studyingAll.put(userId, studyId)
      if (contributor && public) {
        val wasAlreadyInStudy = online isStudying userId
        online.studying.put(userId, studyId)
        if (!wasAlreadyInStudy) notifyFollowersFriendInStudyStateChanged(userId, studyId, "following_joined_study")
      }

    case lila.hub.actorApi.study.StudyDoor(userId, studyId, contributor, public, false) =>
      online.studyingAll invalidate userId
      if (contributor && public) {
        online.studying invalidate userId
        notifyFollowersFriendInStudyStateChanged(userId, studyId, "following_left_study")
      }

    case lila.hub.actorApi.study.StudyBecamePrivate(studyId, contributors) =>
      studyBecamePrivateOrDeleted(studyId, contributors)

    case lila.hub.actorApi.study.RemoveStudy(studyId, contributors) =>
      studyBecamePrivateOrDeleted(studyId, contributors)

    case lila.hub.actorApi.study.StudyBecamePublic(studyId, contributors) =>
      contributorsIn(contributors, studyId) foreach { c =>
        online.studying.put(c, studyId)
        notifyFollowersFriendInStudyStateChanged(c, studyId, "following_joined_study")
      }

    case lila.hub.actorApi.study.StudyMemberGotWriteAccess(userId, studyId) =>
      if (online.isStudyingOrWatching(userId, studyId)) {
        online.studying.put(userId, studyId)
        notifyFollowersFriendInStudyStateChanged(userId, studyId, "following_joined_study")
      }

    case lila.hub.actorApi.study.StudyMemberLostWriteAccess(userId, studyId) =>
      if (online.isStudying(userId, studyId)) {
        online.studying invalidate userId
        notifyFollowersFriendInStudyStateChanged(userId, studyId, "following_left_study")
      }
  }

  private def studyBecamePrivateOrDeleted(studyId: String, contributors: Set[ID]) = {
    contributorsIn(contributors, studyId) foreach { c =>
      online.studying invalidate c
      notifyFollowersFriendInStudyStateChanged(c, studyId, "following_left_study")
    }
  }

  private def contributorsIn(contributors: Set[ID], studyId: String) = {
    val found = online.studying.getAllPresent(contributors).filter(_._2 == studyId)
    contributors filter found.contains
  }

  private def notifyFollowersFriendEnters(
    friendsEntering: List[FriendEntering],
    onlineUserIds: Set[User.ID]
  ) =
    friendsEntering foreach { entering =>
      api fetchFollowersFromSecondary entering.user.id map onlineUserIds.intersect foreach { ids =>
        if (ids.nonEmpty) Bus.publish(SendTos(ids.toSet, JsonView.writeFriendEntering(entering)), 'socketUsers)
      }
    }

  private def notifyFollowersFriendLeaves(
    friendsLeaving: List[LightUser],
    onlineUserIds: Set[User.ID]
  ) =
    friendsLeaving foreach { leaving =>
      api fetchFollowersFromSecondary leaving.id map onlineUserIds.intersect foreach { ids =>
        if (ids.nonEmpty) Bus.publish(SendTos(ids.toSet, "following_leaves", leaving.titleName), 'socketUsers)
      }
    }

  private def notifyFollowersGameStateChanged(userIds: Traversable[ID], message: String) =
    userIds foreach { userId =>
      api.fetchFollowersFromSecondary(userId) map online.userIds().intersect foreach { ids =>
        if (ids.nonEmpty) Bus.publish(SendTos(ids.toSet, message, userId), 'socketUsers)
      }
    }

  private def notifyFollowersFriendInStudyStateChanged(userId: ID, studyId: String, message: String) =
    api.fetchFollowersFromSecondary(userId) map online.userIds().intersect foreach { ids =>
      if (ids.nonEmpty) Bus.publish(SendTos(ids.toSet, message, userId), 'socketUsers)
    }
}
