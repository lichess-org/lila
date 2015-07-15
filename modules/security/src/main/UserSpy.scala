package lila.security

import scala.concurrent.Future

import play.api.mvc.RequestHeader
import reactivemongo.bson._

import lila.common.PimpedJson._
import lila.db.api._
import lila.user.{ User, UserRepo }
import tube.storeColl

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

  case class IPData(ip: IP, blocked: Boolean, location: Location, tor: Boolean)

  private[security] def apply(firewall: Firewall, geoIP: GeoIP)(userId: String): Fu[UserSpy] = for {
    user ← UserRepo named userId flatten "[spy] user not found"
    infos ← Store.findInfoByUser(user.id)
    ips = infos.map(_.ip).distinct
    blockedIps ← (ips map firewall.blocksIp).sequenceFu
    tors = ips.map { ip =>
      infos.exists { x => x.ip == ip && x.isTorExitNode }
    }
    locations <- scala.concurrent.Future {
      ips zip tors map {
        case (_, true) => Location.tor
        case (ip, _)   => geoIP orUnknown ip
      }
    }
    users ← explore(Set(user), Set.empty, Set(user))
  } yield UserSpy(
    ips = ips zip blockedIps zip locations zip tors map {
      case (((ip, blocked), location), tor) => IPData(ip, blocked, location, tor)
    },
    uas = infos.map(_.ua).distinct,
    otherUsers = (users + user).toList.sortBy(-_.createdAt.getMillis))

  private def explore(users: Set[User], ips: Set[IP], _users: Set[User]): Fu[Set[User]] = {
    nextIps(users, ips) flatMap { nIps =>
      nextUsers(nIps, users) map { _ ++: users ++: _users }
    }
  }

  private def nextIps(users: Set[User], ips: Set[IP]): Fu[Set[IP]] =
    users.nonEmpty ?? {
      storeColl.find(
        BSONDocument(
          "user" -> BSONDocument("$in" -> users.map(_.id)),
          "ip" -> BSONDocument("$nin" -> ips)
        ),
        BSONDocument("ip" -> true)
      ).cursor[BSONDocument]().collect[List]() map {
          _.flatMap(_.getAs[IP]("ip")).toSet
        }
    }

  private def nextUsers(ips: Set[IP], users: Set[User]): Fu[Set[User]] =
    ips.nonEmpty ?? {
      storeColl.find(
        BSONDocument(
          "ip" -> BSONDocument("$in" -> ips),
          "user" -> BSONDocument("$nin" -> users.map(_.id))
        ),
        BSONDocument("user" -> true)
      ).cursor[BSONDocument]().collect[List]() map {
          _.flatMap(_.getAs[String]("user"))
        } flatMap { userIds =>
          userIds.nonEmpty ?? (UserRepo byIds userIds.distinct) map (_.toSet)
        }
    }
}
