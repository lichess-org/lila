package lila.security

import scala.collection.breakOut
import reactivemongo.api.ReadPreference
import org.joda.time.DateTime

import lila.common.IpAddress
import lila.db.dsl._
import lila.user.{ User, UserRepo }

case class UserSpy(
    ips: List[UserSpy.IPData],
    uas: List[Store.Dated[String]],
    prints: List[Store.Dated[FingerHash]],
    usersSharingIp: Set[User],
    usersSharingFingerprint: Set[User]
) {

  import UserSpy.OtherUser

  def rawIps = ips map (_.ip.value)

  def ipsByLocations: List[(Location, List[UserSpy.IPData])] =
    ips.sortBy(_.ip).groupBy(_.location).toList.sortBy(_._1.comparable)

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
    infos ← Store.chronoInfoByUser(user.id)
    ips = distinctRecent(infos.map(_.datedIp))
    prints = distinctRecent(infos.flatMap(_.datedFp))
    sharingIp ← exploreSimilar("ip")(user)(coll)
    sharingFingerprint ← exploreSimilar("fp")(user)(coll)
  } yield UserSpy(
    ips = ips map { ip =>
      IPData(ip, firewall blocksIp ip.value, geoIP orUnknown ip.value)
    },
    uas = distinctRecent(infos.map(_.datedUa)),
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
        _.flatMap(_.getAs[Value](field))(breakOut)
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

  import Store.Dated

  case class OtherUser(user: User, byIp: Boolean, byFingerprint: Boolean)

  // distinct values, keeping the most recent of duplicated values
  // assumes all is sorted by most recent first
  def distinctRecent[V](all: List[Dated[V]]): List[Dated[V]] =
    all.foldLeft(Map.empty[V, DateTime]) {
      case (acc, Dated(v, _)) if acc.contains(v) => acc
      case (acc, Dated(v, date)) => acc + (v -> date)
    }.map { case (v, date) => Dated(v, date) }(breakOut)

  type Value = String

  case class IPData(ip: Dated[IpAddress], blocked: Boolean, location: Location)
}
