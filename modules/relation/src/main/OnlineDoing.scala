package lila.relation

import scala.concurrent.duration._

import actorApi.OnlineFriends
import lila.user.User
import lila.memo.CacheApi

final class OnlineDoing(
    api: RelationApi,
    lightUser: lila.common.LightUser.GetterSync,
    enabled: FriendListEnabled,
    val userIds: () => Set[User.ID]
)(implicit ec: scala.concurrent.ExecutionContext) {

  private type StudyId = String

  val playing = new lila.memo.ExpireSetMemo(4 hours)

  def isPlaying(userId: User.ID) = playing get userId

  // people with write access in public studies
  private[relation] val studying = CacheApi.scaffeineNoScheduler
    .expireAfterAccess(20 minutes)
    .build[ID, StudyId]

  // people with write or read access in public and private studies
  private[relation] val studyingAll = CacheApi.scaffeineNoScheduler
    .expireAfterAccess(20 minutes)
    .build[ID, StudyId]

  private[relation] def isStudying(userId: User.ID) = studying.getIfPresent(userId).isDefined

  private[relation] def isStudying(userId: User.ID, studyId: StudyId) =
    studying.getIfPresent(userId) has studyId

  private[relation] def isStudyingOrWatching(userId: User.ID, studyId: StudyId) =
    studyingAll.getIfPresent(userId) has studyId

  def friendsOf(userId: User.ID): Fu[OnlineFriends] =
    if (enabled()) api fetchFollowing userId map userIds().intersect map { friends =>
      if (friends.isEmpty) OnlineFriends.empty
      else
        OnlineFriends(
          users = friends.view.flatMap { lightUser(_) }.toList,
          playing = playing intersect friends,
          studying = friends filter studying.getAllPresent(friends).contains
        )
    } else fuccess(OnlineFriends.empty)
}
