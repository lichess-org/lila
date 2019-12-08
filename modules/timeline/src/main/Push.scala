package lila.timeline

import akka.actor._
import org.joda.time.DateTime

import lila.hub.actorApi.timeline.propagation._
import lila.hub.actorApi.timeline.{ Propagate, Atom, ReloadTimelines }
import lila.security.Permission
import lila.user.{ User, UserRepo }

private[timeline] final class Push(
    relationApi: lila.relation.RelationApi,
    userRepo: UserRepo,
    entryApi: EntryApi,
    unsubApi: UnsubApi
) extends Actor {

  def receive = {

    case Propagate(data, propagations) =>
      propagate(propagations) flatMap { users =>
        unsubApi.filterUnsub(data.channel, users)
      } foreach { users =>
        if (users.nonEmpty) makeEntry(users, data) >>-
          lila.common.Bus.publish(ReloadTimelines(users), "lobbySocket")
        lila.mon.timeline.notification(users.size)
      }
  }

  private def propagate(propagations: List[Propagation]): Fu[List[User.ID]] =
    propagations.map {
      case Users(ids) => fuccess(ids)
      case Followers(id) => relationApi.fetchFollowersFromSecondary(id)
      case Friends(id) => relationApi.fetchFriends(id)
      case ExceptUser(_) => fuccess(Nil)
      case ModsOnly(_) => fuccess(Nil)
    }.sequence flatMap { users =>
      propagations.foldLeft(fuccess(users.flatten.distinct)) {
        case (fus, ExceptUser(id)) => fus.map(_.filter(id!=))
        case (fus, ModsOnly(true)) => for {
          us <- fus
          userIds <- userRepo.userIdsWithRoles(modPermissions.map(_.name))
        } yield us filter userIds.contains
        case (fus, _) => fus
      }
    }

  private def modPermissions = List(
    Permission.ModNote,
    Permission.Hunter,
    Permission.Admin,
    Permission.SuperAdmin
  )

  private def makeEntry(users: List[User.ID], data: Atom): Fu[Entry] = {
    val entry = Entry.make(data)
    entryApi.findRecent(entry.typ, DateTime.now minusMinutes 60, 1000) flatMap { entries =>
      if (entries.exists(_ similarTo entry)) fufail[Entry]("[timeline] a similar entry already exists")
      else entryApi insert Entry.ForUsers(entry, users) inject entry
    }
  }
}
