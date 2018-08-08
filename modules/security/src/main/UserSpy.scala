package lidraughts.security

import reactivemongo.api.ReadPreference

import lidraughts.common.IpAddress
import lidraughts.db.dsl._
import lidraughts.user.{ User, UserRepo }

case class UserSpy(
    ips: List[UserSpy.IPData],
    uas: List[String],
    prints: List[FingerHash],
    usersSharingIp: Set[User],
    usersSharingFingerprint: Set[User]
) {

  import UserSpy.OtherUser

  def ipStrings = ips map (_.ip)

  def ipsByLocations: List[(Location, List[UserSpy.IPData])] =
    ips.sortBy(_.ip.value).groupBy(_.location).toList.sortBy(_._1.comparable)

  lazy val otherUsers: Set[OtherUser] = {
    usersSharingIp.map { u =>
      OtherUser(u, true, usersSharingFingerprint contains u)
    } ++ usersSharingFingerprint.filterNot(usersSharingIp.contains).map {
      OtherUser(_, false, true)
    }
  }

  def withMeSorted(me: User): List[OtherUser] =
    (OtherUser(me, true, true) :: otherUsers.toList).sortBy(-_.user.createdAt.getMillis)

  def otherUserIds = otherUsers.map(_.user.id)
}

private[security] final class UserSpyApi(firewall: Firewall, geoIP: GeoIP, coll: Coll) {

  import UserSpy._

  def apply(user: User): Fu[UserSpy] = for {
    infos ← Store.findInfoByUser(user.id)
    ips = infos.map(_.ip).distinct
    prints = infos.flatMap(_.fp).map(FingerHash(_)).distinct
    sharingIp ← exploreSimilar("ip")(user)(coll)
    sharingFingerprint ← exploreSimilar("fp")(user)(coll)
  } yield UserSpy(
    ips = ips map { ip =>
      IPData(ip, firewall blocksIp ip, geoIP orUnknown ip)
    },
    uas = infos.map(_.ua).distinct,
    prints = prints,
    usersSharingIp = sharingIp,
    usersSharingFingerprint = sharingFingerprint
  )

  private def exploreSimilar(field: String)(user: User)(implicit coll: Coll): Fu[Set[User]] =
    nextValues(field)(user) flatMap { nValues =>
      nextUsers(field)(nValues, user)
    }

  private def nextValues(field: String)(user: User)(implicit coll: Coll): Fu[Set[Value]] =
    coll.find(
      $doc("user" -> user.id),
      $doc(field -> true)
    ).cursor[Bdoc]().gather[List]() map {
        _.flatMap(_.getAs[Value](field))(scala.collection.breakOut)
      }

  private def nextUsers(field: String)(values: Set[Value], user: User)(implicit coll: Coll): Fu[Set[User]] =
    values.nonEmpty ?? {
      coll.distinctWithReadPreference[String, Set](
        "user",
        $doc(
          field $in values,
          "user" $ne user.id
        ).some,
        ReadPreference.secondaryPreferred
      ) flatMap { userIds =>
          userIds.nonEmpty ?? (UserRepo byIds userIds) map (_.toSet)
        }
    }
}

object UserSpy {

  case class OtherUser(user: User, byIp: Boolean, byFingerprint: Boolean)

  type Fingerprint = String
  type Value = String

  case class IPData(ip: IpAddress, blocked: Boolean, location: Location)
}
