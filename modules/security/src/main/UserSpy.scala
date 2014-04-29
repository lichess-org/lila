package lila.security

import scala.concurrent.Future

import com.github.nscala_time.time.Imports._
import play.api.libs.json._
import play.api.mvc.RequestHeader
import play.modules.reactivemongo.json.ImplicitBSONHandlers._

import lila.common.PimpedJson._
import lila.db.api._
import lila.db.Types.Coll
import lila.user.{ User, UserRepo }
import tube.storeTube

case class UserSpy(
    ips: List[UserSpy.IPData],
    uas: List[String],
    otherUsers: List[User]) {

  def ipStrings = ips map (_.ip)

  def ipsByLocations: List[(Location, List[UserSpy.IPData])] =
    ips.sortBy(_.ip).groupBy(_.location).toList.sortBy(_._1.comparable)
}

object UserSpy {

  type IP = String

  case class IPData(ip: IP, blocked: Boolean, location: Location)

  private[security] def apply(firewall: Firewall, geoIP: GeoIP)(userId: String): Fu[UserSpy] = for {
    user ← UserRepo named userId flatten "[spy] user not found"
    objs ← $find(Json.obj("user" -> user.id))
    ips = objs.flatMap(_ str "ip").distinct
    blockedIps ← (ips map firewall.blocksIp).sequenceFu
    locations <- (ips map geoIP.apply).sequenceFu
    users ← explore(Set(user), Set.empty, Set(user))
  } yield UserSpy(
    ips = ips zip blockedIps zip locations map {
      case ((ip, blocked), location) => IPData(ip, blocked, location)
    },
    uas = objs.map(_ str "ua").flatten.distinct,
    otherUsers = (users + user).toList.sortBy(_.createdAt)
  )

  private def explore(users: Set[User], ips: Set[IP], _users: Set[User]): Fu[Set[User]] = {
    nextIps(users, ips) flatMap { nIps =>
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
      )(_.asOpt[String]) flatMap { userIds =>
          userIds.nonEmpty ?? (UserRepo byIds userIds.distinct) map (_.toSet)
        }
    }
}
