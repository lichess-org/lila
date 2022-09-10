package lila.timeline

import akka.actor._
import org.joda.time.DateTime
import scala.concurrent.duration._

import lila.common.config.Max
import lila.hub.actorApi.timeline.propagation._
import lila.hub.actorApi.timeline.{ Atom, Propagate, ReloadTimelines }
import lila.security.Permission
import lila.user.{ User, UserRepo }

final private[timeline] class TimelinePush(
    relationApi: lila.relation.RelationApi,
    userRepo: UserRepo,
    entryApi: EntryApi,
    unsubApi: UnsubApi,
    memberRepo: lila.team.MemberRepo,
    teamCache: lila.team.Cached
) extends Actor {

  implicit def ec = context.dispatcher

  private val dedup = lila.memo.OnceEvery.hashCode[Atom](10 minutes)

  def receive = { case Propagate(data, propagations) =>
    if (dedup(data)) propagate(propagations) flatMap { users =>
      unsubApi.filterUnsub(data.channel, users)
    } foreach { users =>
      if (users.nonEmpty)
        insertEntry(users, data) >>-
          lila.common.Bus.publish(ReloadTimelines(users), "lobbySocket")
      lila.mon.timeline.notification.increment(users.size)
    }
  }

  private def propagate(propagations: List[Propagation]): Fu[List[User.ID]] =
    scala.concurrent.Future.traverse(propagations) {
      case Users(ids)    => fuccess(ids)
      case Followers(id) => relationApi.freshFollowersFromSecondary(id)
      case Friends(id)   => relationApi.fetchFriends(id)
      case WithTeam(_)   => fuccess(Nil)
      case ExceptUser(_) => fuccess(Nil)
      case ModsOnly(_)   => fuccess(Nil)
    } flatMap { users =>
      propagations.foldLeft(fuccess(users.flatten.distinct)) {
        case (fus, ExceptUser(id)) => fus.dmap(_.filter(id !=))
        case (fus, ModsOnly(true)) =>
          fus flatMap { us =>
            userRepo.userIdsWithRoles(modPermissions.map(_.dbKey)) dmap { userIds =>
              us filter userIds.contains
            }
          }
        case (fus, WithTeam(teamId)) =>
          teamCache.forumAccess get teamId flatMap {
            case lila.team.Team.Access.MEMBERS =>
              fus flatMap { us =>
                memberRepo.filterUserIdsInTeam(teamId, us).map(_.toList)
              }
            case lila.team.Team.Access.LEADERS =>
              fus flatMap { us =>
                teamCache.leaders.get(teamId).map(us.toSet.intersect).map(_.toList)
              }
            case _ => fus
          }
        case (fus, _) => fus
      }
    }

  private def modPermissions =
    List(
      Permission.ModNote,
      Permission.Admin,
      Permission.SuperAdmin
    )

  private def insertEntry(users: List[User.ID], data: Atom): Funit =
    entryApi insert Entry.ForUsers(Entry.make(data), users)
}
