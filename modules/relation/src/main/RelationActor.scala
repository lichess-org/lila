package lila.relation

import akka.actor.Actor
import scala.concurrent.duration._

import actorApi._
import lila.common.{ Bus, LightUser }
import lila.hub.actorApi.relation._
import lila.hub.actorApi.socket.{ SendTo, SendTos }
import lila.user.User

final private[relation] class RelationActor(
    lightUser: LightUser.GetterSync,
    api: RelationApi,
    online: OnlineDoing
)(implicit ec: scala.concurrent.ExecutionContext)
    extends Actor {

  private var previousOnlineIds = Set.empty[ID]

  private val subs = List("startGame", "finishGame", "study", "reloadOnlineFriends")

  override def preStart(): Unit = {
    Bus.subscribe(self, subs)
    context.system.scheduler.scheduleOnce(15 seconds, self, ComputeMovement)
  }

  override def postStop(): Unit = {
    super.postStop()
    Bus.unsubscribe(self, subs)
  }

  def scheduleNext =
    context.system.scheduler.scheduleOnce(1 seconds, self, ComputeMovement)

  def receive = {

    case ComputeMovement =>
      lila.common.Chronometer.syncMon(_.relation.actor.computeMovementSync) {
        val curIds = online.userIds()
        val leaveUsers: List[LightUser] =
          (previousOnlineIds diff curIds).view.flatMap { lightUser(_) }.to(List)
        val enterUsers: List[LightUser] =
          (curIds diff previousOnlineIds).view.flatMap { lightUser(_) }.to(List)

        val friendsEntering = enterUsers map { u =>
          FriendEntering(u, online.playing get u.id, online isStudying u.id)
        }

        previousOnlineIds = curIds

        notifyFollowersFriendEnters(friendsEntering, curIds)
          .>>(notifyFollowersFriendLeaves(leaveUsers, curIds))
          .mon(_.relation.actor.computeMovement)
          .addEffectAnyway(scheduleNext)
      }

    // triggers following reloading for this user id
    case ReloadOnlineFriends(userId) =>
      online friendsOf userId map { res =>
        // the mobile app requests this on every WS connection
        // we can skip it if empty
        if (!res.isEmpty) Bus.publish(SendTo(userId, JsonView writeOnlineFriends res), "socketUsers")
      } mon (_.relation.actor.reloadFriends)

    case lila.game.actorApi.FinishGame(game, _, _) if game.hasClock =>
      game.userIds.some.filter(_.nonEmpty) foreach { usersPlaying =>
        online.playing removeAll usersPlaying
        notifyFollowersGameStateChanged(usersPlaying, "following_stopped_playing")
      }

    case lila.game.actorApi.StartGame(game) if game.hasClock =>
      game.userIds.some.filter(_.nonEmpty) foreach { usersPlaying =>
        online.playing putAll usersPlaying
        notifyFollowersGameStateChanged(usersPlaying, "following_playing")
      }

    case lila.hub.actorApi.study.StudyDoor(userId, studyId, contributor, public, true) =>
      online.studyingAll.put(userId, studyId)
      if (contributor && public) {
        val wasAlreadyInStudy = online isStudying userId
        online.studying.put(userId, studyId)
        if (!wasAlreadyInStudy) notifyFollowersFriendInStudyStateChanged(userId, "following_joined_study")
      }

    case lila.hub.actorApi.study.StudyDoor(userId, _, contributor, public, false) =>
      online.studyingAll invalidate userId
      if (contributor && public) {
        online.studying invalidate userId
        notifyFollowersFriendInStudyStateChanged(userId, "following_left_study")
      }

    case lila.hub.actorApi.study.StudyBecamePrivate(studyId, contributors) =>
      studyBecamePrivateOrDeleted(studyId, contributors)

    case lila.hub.actorApi.study.RemoveStudy(studyId, contributors) =>
      studyBecamePrivateOrDeleted(studyId, contributors)

    case lila.hub.actorApi.study.StudyBecamePublic(studyId, contributors) =>
      contributorsIn(contributors, studyId) foreach { c =>
        online.studying.put(c, studyId)
        notifyFollowersFriendInStudyStateChanged(c, "following_joined_study")
      }

    case lila.hub.actorApi.study.StudyMemberGotWriteAccess(userId, studyId) =>
      if (online.isStudyingOrWatching(userId, studyId)) {
        online.studying.put(userId, studyId)
        notifyFollowersFriendInStudyStateChanged(userId, "following_joined_study")
      }

    case lila.hub.actorApi.study.StudyMemberLostWriteAccess(userId, studyId) =>
      if (online.isStudying(userId, studyId)) {
        online.studying invalidate userId
        notifyFollowersFriendInStudyStateChanged(userId, "following_left_study")
      }
  }

  private def studyBecamePrivateOrDeleted(studyId: String, contributors: Set[ID]) = {
    contributorsIn(contributors, studyId) foreach { c =>
      online.studying invalidate c
      notifyFollowersFriendInStudyStateChanged(c, "following_left_study")
    }
  }

  private def contributorsIn(contributors: Set[ID], studyId: String) = {
    val found = online.studying.getAllPresent(contributors).filter(_._2 == studyId)
    contributors filter found.contains
  }

  private def notifyFollowersFriendEnters(
      friendsEntering: List[FriendEntering],
      onlineUserIds: Set[User.ID]
  ): Funit =
    friendsEntering
      .map { entering =>
        api fetchFollowersFromSecondary entering.user.id map onlineUserIds.intersect map { ids =>
          if (ids.nonEmpty) Bus.publish(SendTos(ids, JsonView.writeFriendEntering(entering)), "socketUsers")
        }
      }
      .sequenceFu
      .void

  private def notifyFollowersFriendLeaves(
      friendsLeaving: List[LightUser],
      onlineUserIds: Set[User.ID]
  ): Funit =
    friendsLeaving
      .map { leaving =>
        api fetchFollowersFromSecondary leaving.id map onlineUserIds.intersect map { ids =>
          if (ids.nonEmpty) Bus.publish(SendTos(ids, "following_leaves", leaving.titleName), "socketUsers")
        }
      }
      .sequenceFu
      .void

  private def notifyFollowersGameStateChanged(userIds: Iterable[ID], message: String) = {
    val onlineIds = online.userIds()
    userIds
      .map { userId =>
        api.fetchFollowersFromSecondary(userId) map onlineIds.intersect map { ids =>
          if (ids.nonEmpty) Bus.publish(SendTos(ids, message, userId), "socketUsers")
        }
      }
      .sequenceFu
      .mon(_.relation.actor.gameStateChanged)
  }

  private def notifyFollowersFriendInStudyStateChanged(userId: ID, message: String) =
    api
      .fetchFollowersFromSecondary(userId)
      .map {
        online.userIds().intersect
      }
      .map { ids =>
        if (ids.nonEmpty) Bus.publish(SendTos(ids, message, userId), "socketUsers")
      }
      .mon(_.relation.actor.studyStateChanged)
}
