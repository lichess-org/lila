package lila.timeline

import akka.actor.*

import lila.hub.actorApi.timeline.{ Propagation, Atom, Propagate, ReloadTimelines }
import lila.security.Permission
import lila.user.UserRepo

final private[timeline] class TimelinePush(
    relationApi: lila.relation.RelationApi,
    userRepo: UserRepo,
    entryApi: EntryApi,
    unsubApi: UnsubApi,
    memberRepo: lila.team.TeamMemberRepo,
    teamCache: lila.team.Cached,
    teamMemberRepo: lila.team.TeamMemberRepo
) extends Actor:

  private given Executor = context.dispatcher

  private val dedup = lila.memo.OnceEvery.hashCode[Atom](10 minutes)

  def receive = { case Propagate(data, propagations) =>
    if dedup(data) then
      propagate(propagations)
        .flatMap: users =>
          unsubApi.filterUnsub(data.channel, users)
        .foreach: users =>
          if users.nonEmpty then
            insertEntry(users, data) andDo
              lila.common.Bus.publish(ReloadTimelines(users), "lobbySocket")
          lila.mon.timeline.notification.increment(users.size)
  }

  private def propagate(propagations: List[Propagation]): Fu[List[UserId]] =
    Future
      .traverse(propagations):
        case Propagation.Users(ids)    => fuccess(ids)
        case Propagation.Followers(id) => relationApi.freshFollowersFromSecondary(id)
        case Propagation.Friends(id)   => relationApi.fetchFriends(id)
        case Propagation.WithTeam(_)   => fuccess(Nil)
        case Propagation.ExceptUser(_) => fuccess(Nil)
        case Propagation.ModsOnly(_)   => fuccess(Nil)
      .flatMap: users =>
        propagations.foldLeft(fuccess(users.flatten.distinct)) {
          case (fus, Propagation.ExceptUser(id)) => fus.dmap(_.filter(id !=))
          case (fus, Propagation.ModsOnly(true)) =>
            fus.flatMap: us =>
              userRepo.userIdsWithRoles(modPermissions.map(_.dbKey)) dmap { userIds =>
                us filter userIds.contains
              }
          case (fus, Propagation.WithTeam(teamId)) =>
            teamCache.forumAccess get teamId flatMap {
              case lila.team.Team.Access.MEMBERS =>
                fus.flatMap: us =>
                  memberRepo.filterUserIdsInTeam(teamId, us).map(_.toList)
              case lila.team.Team.Access.LEADERS =>
                fus.flatMap: us =>
                  teamMemberRepo.leaderIds(teamId).map(us.toSet.intersect).map(_.toList)
              case _ => fus
            }
          case (fus, _) => fus
        }

  private def modPermissions =
    List(
      Permission.ModNote,
      Permission.Admin,
      Permission.SuperAdmin
    )

  private def insertEntry(users: List[UserId], data: Atom): Funit =
    entryApi insert Entry.ForUsers(Entry.make(data), users)
