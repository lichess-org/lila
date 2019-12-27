package lila.relation

import scala.concurrent.duration._

import actorApi.OnlineFriends
import lila.user.User
import lila.memo.CacheApi

final class OnlineDoing(
    api: RelationApi,
    lightUser: lila.common.LightUser.GetterSync,
    val userIds: () => Set[User.ID]
)(implicit ec: scala.concurrent.ExecutionContext) {

  private type StudyId = String

  val playing = new lila.memo.ExpireSetMemo(4 hours)

  def isPlaying(userId: User.ID) = playing get userId

  // people with write access in public studies
  val studying = CacheApi.scaffeineNoScheduler
    .expireAfterAccess(20 minutes)
    .build[ID, StudyId]

  // people with write or read access in public and private studies
  val studyingAll = CacheApi.scaffeineNoScheduler
    .expireAfterAccess(20 minutes)
    .build[ID, StudyId]

  def isStudying(userId: User.ID) = studying.getIfPresent(userId).isDefined

  def isStudying(userId: User.ID, studyId: StudyId) = studying.getIfPresent(userId) has studyId

  def isStudyingOrWatching(userId: User.ID, studyId: StudyId) = studyingAll.getIfPresent(userId) has studyId

  def friendsOf(userId: User.ID): Fu[OnlineFriends] =
    api fetchFollowing userId map userIds().intersect map { friends =>
      if (friends.isEmpty) OnlineFriends.empty
      else
        OnlineFriends(
          users = friends.view.flatMap { lightUser(_) }.to(List),
          playing = playing intersect friends,
          studying = friends filter studying.getAllPresent(friends).contains
        )
    }
}
