package lidraughts.relation

import akka.actor.Actor
import scala.collection.breakOut

import actorApi._
import lidraughts.common.LightUser
import lidraughts.hub.actorApi.relation._
import lidraughts.hub.actorApi.{ SendTo, SendTos }

private[relation] final class RelationActor(
    lightUser: LightUser.GetterSync,
    api: RelationApi,
    online: OnlineDoing
) extends Actor {

  private val bus = context.system.lidraughtsBus

  private var previousOnlineIds = Set.empty[ID]

  override def preStart(): Unit = {
    context.system.lidraughtsBus.subscribe(self, 'startGame, 'finishGame, 'study)
  }

  override def postStop(): Unit = {
    super.postStop()
    context.system.lidraughtsBus.unsubscribe(self)
  }

  def receive = {

    case ComputeMovement =>
      val curIds = online.userIds.keySet
      val leaveUsers: List[LightUser] = (previousOnlineIds diff curIds).flatMap { lightUser(_) }(breakOut)
      val enterUsers: List[LightUser] = (curIds diff previousOnlineIds).flatMap { lightUser(_) }(breakOut)

      val friendsEntering = enterUsers map { u =>
        FriendEntering(u, online.playing get u.id, online isStudying u.id)
      }

      notifyFollowersFriendEnters(friendsEntering)
      notifyFollowersFriendLeaves(leaveUsers)
      previousOnlineIds = curIds

    // triggers following reloading for this user id
    case ReloadOnlineFriends(userId) => online friendsOf userId foreach { res =>
      bus.publish(SendTo(userId, JsonView writeOnlineFriends res), 'users)
    }

    case lidraughts.game.actorApi.FinishGame(game, _, _) if game.hasClock =>
      val usersPlaying = game.userIds
      online.playing removeAll usersPlaying
      notifyFollowersGameStateChanged(usersPlaying, "following_stopped_playing")

    case lidraughts.game.actorApi.StartGame(game) if game.hasClock =>
      val usersPlaying = game.userIds
      online.playing putAll usersPlaying
      notifyFollowersGameStateChanged(usersPlaying, "following_playing")

    case lidraughts.hub.actorApi.study.StudyDoor(userId, studyId, contributor, public, true) =>
      online.studyingAll.put(userId, studyId)
      if (contributor && public) {
        val wasAlreadyInStudy = online isStudying userId
        online.studying.put(userId, studyId)
        if (!wasAlreadyInStudy) notifyFollowersFriendInStudyStateChanged(userId, studyId, "following_joined_study")
      }

    case lidraughts.hub.actorApi.study.StudyDoor(userId, studyId, contributor, public, false) =>
      online.studyingAll invalidate userId
      if (contributor && public) {
        online.studying invalidate userId
        notifyFollowersFriendInStudyStateChanged(userId, studyId, "following_left_study")
      }

    case lidraughts.hub.actorApi.study.StudyBecamePrivate(studyId, contributors) =>
      studyBecamePrivateOrDeleted(studyId, contributors)

    case lidraughts.hub.actorApi.study.RemoveStudy(studyId, contributors) =>
      studyBecamePrivateOrDeleted(studyId, contributors)

    case lidraughts.hub.actorApi.study.StudyBecamePublic(studyId, contributors) =>
      contributorsIn(contributors, studyId) foreach { c =>
        online.studying.put(c, studyId)
        notifyFollowersFriendInStudyStateChanged(c, studyId, "following_joined_study")
      }

    case lidraughts.hub.actorApi.study.StudyMemberGotWriteAccess(userId, studyId) =>
      if (online.isStudyingOrWatching(userId, studyId)) {
        online.studying.put(userId, studyId)
        notifyFollowersFriendInStudyStateChanged(userId, studyId, "following_joined_study")
      }

    case lidraughts.hub.actorApi.study.StudyMemberLostWriteAccess(userId, studyId) =>
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

  private def notifyFollowersFriendEnters(friendsEntering: List[FriendEntering]) =
    friendsEntering foreach { entering =>
      api fetchFollowersFromSecondary entering.user.id map online.userIds.intersect foreach { ids =>
        if (ids.nonEmpty) bus.publish(SendTos(ids.toSet, JsonView.writeFriendEntering(entering)), 'users)
      }
    }

  private def notifyFollowersFriendLeaves(friendsLeaving: List[LightUser]) =
    friendsLeaving foreach { leaving =>
      api fetchFollowersFromSecondary leaving.id map online.userIds.intersect foreach { ids =>
        if (ids.nonEmpty) bus.publish(SendTos(ids.toSet, "following_leaves", leaving.titleName), 'users)
      }
    }

  private def notifyFollowersGameStateChanged(userIds: Traversable[ID], message: String) =
    userIds foreach { userId =>
      api.fetchFollowersFromSecondary(userId) map online.userIds.intersect foreach { ids =>
        if (ids.nonEmpty) bus.publish(SendTos(ids.toSet, message, userId), 'users)
      }
    }

  private def notifyFollowersFriendInStudyStateChanged(userId: ID, studyId: String, message: String) =
    api.fetchFollowersFromSecondary(userId) map online.userIds.intersect foreach { ids =>
      if (ids.nonEmpty) bus.publish(SendTos(ids.toSet, message, userId), 'users)
    }
}
