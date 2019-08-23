package lidraughts.security

import org.joda.time.DateTime
import reactivemongo.api.ReadPreference
import scala.collection.breakOut

import lidraughts.common.{ IpAddress, EmailAddress }
import lidraughts.db.dsl._
import lidraughts.user.{ User, UserRepo }

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

  def otherUserIds = otherUsers.map(_.user.id)
}

final class UserSpyApi(firewall: Firewall, geoIP: GeoIP, coll: Coll) {

  import UserSpy._

  def apply(user: User): Fu[UserSpy] = for {
    infos ← Store.chronoInfoByUser(user.id)
    ips = distinctRecent(infos.map(_.datedIp))
    prints = distinctRecent(infos.flatMap(_.datedFp))
    sharingIp ← exploreSimilar("ip")(user)
    sharingFingerprint ← exploreSimilar("fp")(user)
  } yield UserSpy(
    ips = ips map { ip =>
      IPData(ip, firewall blocksIp ip.value, geoIP orUnknown ip.value)
    },
    uas = distinctRecent(infos.map(_.datedUa)),
    prints = prints,
    usersSharingIp = sharingIp,
    usersSharingFingerprint = sharingFingerprint
  )

  private[security] def userHasPrint(u: User): Fu[Boolean] = coll.exists(
    $doc("user" -> u.id, "fp" $exists true),
    readPreference = ReadPreference.secondaryPreferred
  )

  private def exploreSimilar(field: String)(user: User): Fu[Set[User]] =
    nextValues(field)(user.id) flatMap { nValues =>
      nextUsers(field)(nValues, user)
    }

  private def nextValues(field: String)(userId: User.ID): Fu[Set[Value]] =
    coll.find(
      $doc("user" -> userId),
      $doc(field -> true)
    ).list[Bdoc]() map {
        _.flatMap(_.getAs[Value](field))(breakOut)
      }

  private def nextUsers(field: String)(values: Set[Value], user: User): Fu[Set[User]] =
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

  def getUserIdsWithSameIpAndPrint(userId: User.ID): Fu[Set[User.ID]] = for {
    ips <- nextValues("ip")(userId)
    fps <- nextValues("fp")(userId)
    users <- (ips.nonEmpty && fps.nonEmpty) ?? coll.distinctWithReadPreference[User.ID, Set](
      "user",
      $doc(
        "ip" $in ips,
        "fp" $in fps,
        "user" $ne userId
      ).some,
      ReadPreference.secondaryPreferred
    )
  } yield users
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

  case class WithMeSortedWithEmails(others: List[OtherUser], emails: Map[User.ID, EmailAddress]) {
    def emailValueOf(u: User) = emails.get(u.id).map(_.value)
  }

  def withMeSortedWithEmails(me: User, others: Set[OtherUser]): Fu[WithMeSortedWithEmails] = {
    val othersList = others.toList
    lidraughts.user.UserRepo.emailMap(me.id :: othersList.map(_.user.id)) map { emailMap =>
      WithMeSortedWithEmails(
        (OtherUser(me, true, true) :: othersList).sortBy(-_.user.createdAt.getMillis),
        emailMap
      )
    }
  }
}
