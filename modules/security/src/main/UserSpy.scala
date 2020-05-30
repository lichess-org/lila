package lila.security

import org.joda.time.DateTime
import reactivemongo.api.ReadPreference
import reactivemongo.api.bson._

import lila.common.{ EmailAddress, IpAddress }
import lila.db.dsl._
import lila.user.{ User, UserRepo }

case class UserSpy(
    ips: List[UserSpy.IPData],
    uas: List[Store.Dated[String]],
    prints: List[Store.Dated[FingerHash]],
    usersSharingIp: List[User],
    usersSharingFingerprint: List[User]
) {

  import UserSpy.OtherUser

  def rawIps = ips map (_.ip.value)

  def ipsByLocations: List[(Location, List[UserSpy.IPData])] =
    ips.sortBy(_.ip).groupBy(_.location).toList.sortBy(_._1.comparable)

  lazy val otherUsers: List[OtherUser] = {
    val ipIds = usersSharingIp.view.map(_.id).toSet
    val fpIds = usersSharingFingerprint.view.map(_.id).toSet
    usersSharingIp.map { u =>
      OtherUser(u, true, fpIds contains u.id)
    } ++ usersSharingFingerprint.filterNot(u => ipIds.contains(u.id)).map {
      OtherUser(_, false, true)
    }
  }

  def otherUserIds = otherUsers.map(_.user.id)
}

final class UserSpyApi(
    firewall: Firewall,
    store: Store,
    userRepo: UserRepo,
    geoIP: GeoIP,
    ip2proxy: Ip2Proxy
)(implicit ec: scala.concurrent.ExecutionContext) {

  import UserSpy._

  def apply(user: User): Fu[UserSpy] =
    store.chronoInfoByUser(user) flatMap { infos =>
      val ips    = distinctRecent(infos.map(_.datedIp))
      val prints = distinctRecent(infos.flatMap(_.datedFp))
      nextUsers("ip", ips.map(_.value.value).toList, user) zip
        nextUsers("fp", prints.map(_.value.value).toList, user) zip
        ip2proxy.keepProxies(ips.map(_.value).toList) map {
        case sharingIp ~ sharingFingerprint ~ proxies =>
          UserSpy(
            ips = ips.map { ip =>
              IPData(ip, firewall blocksIp ip.value, geoIP orUnknown ip.value, proxies(ip.value))
            }.toList,
            uas = distinctRecent(infos.map(_.datedUa)).toList,
            prints = prints.toList,
            usersSharingIp = sharingIp,
            usersSharingFingerprint = sharingFingerprint
          )
      }
    }

  private[security] def userHasPrint(u: User): Fu[Boolean] =
    store.coll.secondaryPreferred.exists(
      $doc("user" -> u.id, "fp" $exists true)
    )

  private def nextValues(field: String)(userId: User.ID): Fu[Set[String]] =
    store.coll
      .find(
        $doc("user" -> userId),
        $doc(field -> true).some
      )
      .list[Bdoc](500, ReadPreference.secondaryPreferred)
      .map {
        _.view.flatMap(_ string field).to(Set)
      }

  private def nextUsers(field: String, values: Seq[String], user: User): Fu[List[User]] =
    values.nonEmpty ?? store.coll
      .aggregateList(1000, readPreference = ReadPreference.secondaryPreferred) { implicit framework =>
        import framework._
        Match(
          $doc(
            field $in values,
            "user" $ne user.id,
            "date" $gt DateTime.now.minusYears(1)
          )
        ) -> List(
          Group(BSONNull)("uid" -> AddFieldToSet("user")),
          UnwindField("uid"),
          PipelineOperator(
            $doc(
              "$lookup" -> $doc(
                "from"         -> userRepo.coll.name,
                "localField"   -> "uid",
                "foreignField" -> "_id",
                "as"           -> "user"
              )
            )
          ),
          UnwindField("user")
        )
      }
      .map { docs =>
        import lila.user.User.userBSONHandler
        for {
          doc  <- docs
          user <- doc.getAsOpt[User]("user")
        } yield user
      }

  def getUserIdsWithSameIpAndPrint(userId: User.ID): Fu[Set[User.ID]] =
    for {
      ips <- nextValues("ip")(userId)
      fps <- nextValues("fp")(userId)
      users <- (ips.nonEmpty && fps.nonEmpty) ?? store.coll.secondaryPreferred.distinctEasy[User.ID, Set](
        "user",
        $doc(
          "ip" $in ips,
          "fp" $in fps,
          "user" $ne userId
        )
      )
    } yield users
}

object UserSpy {

  import Store.Dated

  case class OtherUser(user: User, byIp: Boolean, byFingerprint: Boolean)

  // distinct values, keeping the most recent of duplicated values
  // assumes all is sorted by most recent first
  def distinctRecent[V](all: List[Dated[V]]): scala.collection.View[Dated[V]] =
    all
      .foldLeft(Map.empty[V, DateTime]) {
        case (acc, Dated(v, _)) if acc.contains(v) => acc
        case (acc, Dated(v, date))                 => acc + (v -> date)
      }
      .view
      .map { case (v, date) => Dated(v, date) }

  case class IPData(ip: Dated[IpAddress], blocked: Boolean, location: Location, proxy: Boolean)

  case class WithMeSortedWithEmails(others: List[OtherUser], emails: Map[User.ID, EmailAddress]) {
    def emailValueOf(u: User) = emails.get(u.id).map(_.value)
  }

  def withMeSortedWithEmails(
      userRepo: UserRepo,
      me: User,
      others: List[OtherUser]
  )(implicit ec: scala.concurrent.ExecutionContext): Fu[WithMeSortedWithEmails] =
    userRepo.emailMap(me.id :: others.map(_.user.id)) map { emailMap =>
      WithMeSortedWithEmails(
        (OtherUser(me, true, true) :: others).sortBy(-_.user.createdAt.getMillis),
        emailMap
      )
    }
}
