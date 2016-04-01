package lila.security

import scala.concurrent.Future

import play.api.mvc.RequestHeader
import reactivemongo.bson._

import lila.common.PimpedJson._
import lila.db.dsl._
import lila.user.{ User, UserRepo }

case class UserSpy(
    ips: List[UserSpy.IPData],
    uas: List[String],
    usersSharingIp: List[User],
    usersSharingFingerprint: List[User]) {

  import UserSpy.OtherUser

  def ipStrings = ips map (_.ip)

  def ipsByLocations: List[(Location, List[UserSpy.IPData])] =
    ips.sortBy(_.ip).groupBy(_.location).toList.sortBy(_._1.comparable)

  lazy val otherUsers: List[OtherUser] = {
    usersSharingIp.map { u =>
      OtherUser(u, true, usersSharingFingerprint contains u)
    } ::: usersSharingFingerprint.filterNot(usersSharingIp.contains).map {
      OtherUser(_, false, true)
    }
  }.sortBy(-_.user.createdAt.getMillis)
}

object UserSpy {

  case class OtherUser(user: User, byIp: Boolean, byFingerprint: Boolean)

  type IP = String
  type Fingerprint = String
  type Value = String

  case class IPData(ip: IP, blocked: Boolean, location: Location)

  private[security] def apply(firewall: Firewall, geoIP: GeoIP)(coll: Coll)(userId: String): Fu[UserSpy] = for {
    user ← UserRepo named userId flatten "[spy] user not found"
    infos ← Store.findInfoByUser(user.id)
    ips = infos.map(_.ip).distinct
    blockedIps ← (ips map firewall.blocksIp).sequenceFu
    locations <- scala.concurrent.Future {
      ips map geoIP.orUnknown
    }
    sharingIp ← exploreSimilar("ip")(user)(coll)
    sharingFingerprint ← exploreSimilar("fp")(user)(coll)
  } yield UserSpy(
    ips = ips zip blockedIps zip locations map {
      case ((ip, blocked), location) => IPData(ip, blocked, location)
    },
    uas = infos.map(_.ua).distinct,
    usersSharingIp = (sharingIp + user).toList.sortBy(-_.createdAt.getMillis),
    usersSharingFingerprint = (sharingFingerprint + user).toList.sortBy(-_.createdAt.getMillis))

  private def exploreSimilar(field: String)(user: User)(implicit coll: Coll): Fu[Set[User]] =
    nextValues(field)(user) flatMap { nValues =>
      nextUsers(field)(nValues, user) map { _ + user }
    }

  private def nextValues(field: String)(user: User)(implicit coll: Coll): Fu[Set[Value]] =
    coll.find(
      BSONDocument("user" -> user.id),
      BSONDocument(field -> true)
    ).cursor[BSONDocument]().collect[List]() map {
        _.flatMap(_.getAs[Value](field)).toSet
      }

  private def nextUsers(field: String)(values: Set[Value], user: User)(implicit coll: Coll): Fu[Set[User]] =
    values.nonEmpty ?? {
      coll.distinct("user",
        BSONDocument(
          field -> BSONDocument("$in" -> values),
          "user" -> BSONDocument("$ne" -> user.id)
        ).some
      ) map lila.db.BSON.asStrings flatMap { userIds =>
          userIds.nonEmpty ?? (UserRepo byIds userIds) map (_.toSet)
        }
    }
}
