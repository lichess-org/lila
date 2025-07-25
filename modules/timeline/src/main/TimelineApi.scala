package lila.timeline
import lila.core.perm.Permission
import lila.core.team.Access
import lila.core.timeline.*

private final class TimelineApi(
    relationApi: lila.core.relation.RelationApi,
    userApi: lila.core.user.UserApi,
    entryApi: EntryApi,
    unsubApi: UnsubApi,
    teamApi: lila.core.team.TeamApi
)(using Executor):

  private val dedup = scalalib.cache.OnceEvery.hashCode[Atom](10.minutes)

  def apply(propagate: Propagate): Unit =
    import propagate.*
    if dedup(data) then
      doPropagate(propagations)
        .flatMap: users =>
          unsubApi.filterUnsub(data.channel, users)
        .foreach: users =>
          if users.nonEmpty then
            for _ <- insertEntry(users, data)
            yield lila.common.Bus.pub(ReloadTimelines(users))
          lila.mon.timeline.notification.increment(users.size)

  private def doPropagate(propagations: List[Propagation]): Fu[List[UserId]] =
    Future
      .traverse(propagations):
        case Propagation.Users(ids) => fuccess(ids)
        case Propagation.Followers(id) => relationApi.freshFollowersFromSecondary(id)
        case Propagation.Friends(id) => relationApi.fetchFriends(id)
        case Propagation.WithTeam(_) => fuccess(Nil)
        case Propagation.ExceptUser(_) => fuccess(Nil)
        case Propagation.ModsOnly(_) => fuccess(Nil)
      .flatMap: users =>
        propagations.foldLeft(fuccess(users.flatten.distinct)):
          case (fus, Propagation.ExceptUser(id)) => fus.dmap(_.filter(id !=))
          case (fus, Propagation.ModsOnly(true)) =>
            fus.flatMap: us =>
              userApi.userIdsWithRoles(modPermissions.map(_.dbKey)).dmap { userIds =>
                us.filter(userIds.contains)
              }
          case (fus, Propagation.WithTeam(teamId)) =>
            teamApi
              .forumAccessOf(teamId)
              .flatMap:
                case Access.Members =>
                  fus.flatMap: us =>
                    teamApi.filterUserIdsInTeam(teamId, us).map(_.toList)
                case Access.Leaders =>
                  fus.flatMap: us =>
                    teamApi.leaderIds(teamId).map(us.toSet.intersect).map(_.toList)
                case _ => fus
          case (fus, _) => fus

  private def modPermissions =
    List(
      Permission.ModNote,
      Permission.Admin,
      Permission.SuperAdmin
    )

  private def insertEntry(users: List[UserId], data: Atom): Funit =
    entryApi.insert(Entry.ForUsers(Entry.make(data), users))
