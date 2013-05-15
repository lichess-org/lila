package lila.security

import lila.common.PimpedJson._
import lila.user.{ User, UserRepo }
import lila.db.Types.Coll
import lila.db.api._
import tube.storeTube

import play.api.mvc.RequestHeader
import play.api.libs.json._

import play.modules.reactivemongo.json.ImplicitBSONHandlers._

import org.joda.time.DateTime
import scala.concurrent.Future

case class UserSpy(
  ips: List[String],
  uas: List[String],
  otherUsers: List[User])

object Store {

  type IP = String

  def save(sessionId: String, userId: String, req: RequestHeader): Funit =
    $insert(Json.obj(
      "_id" -> sessionId,
      "user" -> userId,
      "ip" -> ip(req),
      "ua" -> ua(req),
      "date" -> DateTime.now,
      "up" -> true))

  def userId(sessionId: String): Fu[Option[String]] =
    $primitive.one(
      $select(sessionId) ++ Json.obj("up" -> true),
      "user"
    )(_.asOpt[String])

  def delete(sessionId: String): Funit =
    $update($select(sessionId), $set("up" -> false))

  // useful when closing an account,
  // we want to logout too
  def deleteUser(userId: String): Funit = $update(
    selectUser(userId),
    $set("up" -> false),
    upsert = false,
    multi = true)

  private[security] def userSpy(userId: String): Fu[UserSpy] = for {
    user ← UserRepo byId userId flatten "[spy] user not found"
    objs ← $find(selectUser(user.id))
    users ← explore(Set(user), Set.empty, Set(user))
  } yield UserSpy(
    ips = objs.map(_ str "ip").flatten.distinct,
    uas = objs.map(_ str "ua").flatten.distinct,
    otherUsers = (users - user).toList.sortBy(_.createdAt)
  )

  private def explore(users: Set[User], ips: Set[IP], _users: Set[User]): Fu[Set[User]] = {
    nextIps(users, ips) flatMap { nIps ⇒
      nextUsers(nIps, users) flatMap { nUsers ⇒
        nUsers.isEmpty ? fuccess(users) | explore(nUsers, nIps ++: ips, nUsers ++: users)
      }
    }
  }

  private def nextIps(users: Set[User], ips: Set[IP]): Fu[Set[IP]] =
    users.nonEmpty ?? {
      $primitive(
        Json.obj("user" -> $in(users.map(_.id)), "ip" -> $nin(ips)), "ip"
      )(_.asOpt[IP]) map (_.toSet)
    }

  private def nextUsers(ips: Set[IP], users: Set[User]): Fu[Set[User]] =
    ips.nonEmpty ?? {
      $primitive(
        Json.obj("ip" -> $in(ips), "user" -> $nin(users.map(_.id))), "user"
      )(_.asOpt[String]) flatMap { userIds ⇒
          userIds.nonEmpty ?? (UserRepo byIds userIds) map (_.toSet)
        }
    }

  private def ip(req: RequestHeader) = req.remoteAddress

  private def ua(req: RequestHeader) = req.headers.get("User-Agent") | "?"

  private def selectUser(userId: String) = Json.obj("user" -> userId)
}
