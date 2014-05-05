package lila.timeline

import akka.actor._
import akka.pattern.{ ask, pipe }
import com.github.nscala_time.time.Imports._
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.templates.Html

import lila.db.api._
import lila.hub.actorApi.lobby.NewForumPost
import lila.hub.actorApi.timeline.propagation._
import lila.hub.actorApi.timeline.{ Propagate, Atom, ForumPost, ReloadTimeline }
import lila.security.Granter
import lila.user.UserRepo
import makeTimeout.short
import tube.entryTube

private[timeline] final class Push(
    lobbySocket: ActorSelection,
    renderer: ActorSelection,
    getFriendIds: String => Fu[Set[String]],
    getFollowerIds: String => Fu[Set[String]]) extends Actor {

  def receive = {

    case Propagate(data, propagations) =>
      data match {
        case _: ForumPost => lobbySocket ! NewForumPost
        case _            =>
      }
      propagate(propagations) foreach { users =>
        if (users.nonEmpty) makeEntry(users, data) >>-
          (users foreach { u =>
            lobbySocket ! ReloadTimeline(u)
          })
      }
  }

  private def propagate(propagations: List[Propagation]): Fu[List[String]] =
    (propagations map {
      case Users(ids)    => fuccess(ids)
      case Followers(id) => getFollowerIds(id) map (_.toList)
      case Friends(id)   => getFriendIds(id) map (_.toList)
      case StaffFriends(id) => getFriendIds(id) flatMap UserRepo.byIds map {
        _ filter Granter(_.StaffForum) map (_.id)
      }
    }).sequence map (_.flatten.distinct)

  private def makeEntry(users: List[String], data: Atom): Fu[Entry] =
    Entry.make(users, data).fold(
      fufail[Entry]("[timeline] invalid entry data " + data)
    ) { entry =>
        $find(Json.obj("typ" -> entry.typ, "date" -> $gt($date(DateTime.now - 50.minutes)))) flatMap { entries =>
          entries.exists(_ similarTo entry) fold (
            fufail[Entry]("[timeline] a similar entry already exists"),
            $insert(entry) inject entry
          )
        }
      }
}
