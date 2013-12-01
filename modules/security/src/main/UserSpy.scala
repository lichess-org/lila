package lila.security

import scala.concurrent.Future

import play.api.libs.json._
import play.api.mvc.RequestHeader
import play.modules.reactivemongo.json.ImplicitBSONHandlers._

import lila.common.PimpedJson._
import lila.db.api._
import lila.db.Types.Coll
import lila.user.{ User, UserRepo }
import tube.storeTube

case class UserSpy(
    ips: List[(String, Boolean)],
    uas: List[String],
    otherUsers: List[User]) {

  def ipStrings = ips map (_._1)
}

private[security] object UserSpy {

  type IP = String

  private[security] def apply(firewall: Firewall)(userId: String): Fu[UserSpy] = for {
    user ← UserRepo named userId flatten "[spy] user not found"
    objs ← $find(Json.obj("user" -> user.id))
    users ← explore(Set(user), Set.empty, Set(user))
    ips = objs.map(_ str "ip").flatten.distinct
    blockedIps ← (ips map firewall.blocksIp).sequenceFu
  } yield UserSpy(
    ips = ips zip blockedIps,
    uas = objs.map(_ str "ua").flatten.distinct,
    otherUsers = (users + user).toList.sortBy(_.createdAt)
  )

  private def explore(users: Set[User], ips: Set[IP], _users: Set[User]): Fu[Set[User]] = {
    nextIps(users, ips) flatMap { nIps ⇒
      nextUsers(nIps, users) map { _ ++: users ++: _users }
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
          userIds.nonEmpty ?? (UserRepo byIds userIds.distinct) map (_.toSet)
        }
    }
}
