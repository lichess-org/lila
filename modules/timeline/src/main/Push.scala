package lila.timeline

import akka.actor._
import akka.pattern.{ ask, pipe }
import org.joda.time.DateTime
import play.api.libs.json._
import play.twirl.api.Html

import lila.hub.actorApi.lobby.NewForumPost
import lila.hub.actorApi.timeline.propagation._
import lila.hub.actorApi.timeline.{ Propagate, Atom, ForumPost, ReloadTimeline }
import lila.security.{ Granter, Permission }
import lila.user.UserRepo
import makeTimeout.short

private[timeline] final class Push(
    lobbySocket: ActorSelection,
    renderer: ActorSelection,
    getFriendIds: String => Fu[Set[String]],
    getFollowerIds: String => Fu[Set[String]],
    entryRepo: EntryRepo,
    unsubApi: UnsubApi) extends Actor {

  def receive = {

    case Propagate(data, propagations) =>
      data match {
        case _: ForumPost => lobbySocket ! NewForumPost
        case _            =>
      }
      propagate(propagations) flatMap { users =>
        unsubApi.filterUnsub(data.channel, users)
      } foreach { users =>
        if (users.nonEmpty) makeEntry(users, data) >>-
          (users foreach { u =>
            lobbySocket ! ReloadTimeline(u)
          })
        lila.mon.timeline.notification(users.size)
      }
  }

  private def propagate(propagations: List[Propagation]): Fu[List[String]] =
    propagations.map {
      case Users(ids)    => fuccess(ids)
      case Followers(id) => getFollowerIds(id)
      case Friends(id)   => getFriendIds(id)
      case StaffFriends(id) => getFriendIds(id) flatMap UserRepo.byIds map {
        _ filter Granter(_.StaffForum) map (_.id)
      }
      case ExceptUser(_) => fuccess(Nil)
      case ModsOnly(_)   => fuccess(Nil)
    }.sequence flatMap { users =>
      propagations.foldLeft(fuccess(users.flatten.distinct)) {
        case (fus, ExceptUser(id)) => fus.map(_.filter(id!=))
        case (fus, ModsOnly(true)) => for {
          us <- fus
          userIds <- UserRepo.userIdsWithRoles(modPermissions.map(_.name))
        } yield us filter userIds.contains
        case (fus, _) => fus
      }
    }

  private def modPermissions = List(
    Permission.ModNote,
    Permission.Hunter,
    Permission.Admin,
    Permission.SuperAdmin)

  private def makeEntry(users: List[String], data: Atom): Fu[Entry] = {
    val entry = Entry.make(data)
    entryRepo.findRecent(entry.typ, DateTime.now minusMinutes 50) flatMap { entries =>
      entries.exists(_ similarTo entry) fold (
        fufail[Entry]("[timeline] a similar entry already exists"),
        entryRepo insert Entry.ForUsers(entry, users) inject entry
      )
    }
  }
}
