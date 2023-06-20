package lila.security

import reactivemongo.api.bson.*

import lila.common.{ EmailAddress, IpAddress }
import lila.db.dsl.{ *, given }
import lila.user.{ User, UserRepo }

case class UserLogins(
    ips: List[UserLogins.IPData],
    prints: List[UserLogins.FPData],
    uas: List[Dated[UserAgent]],
    otherUsers: List[UserLogins.OtherUser[User]]
):

  import UserLogins.OtherUser

  def rawIps = ips.map(_.ip.value)
  def rawFps = prints.map(_.fp.value)

  def otherUserIds = otherUsers.map(_.userId)

  def usersSharingIp =
    otherUsers.collect {
      case OtherUser(user, ips, _) if ips.nonEmpty => user
    }

  def distinctLocations = UserLogins.distinctRecent(ips.map(_.datedLocation).sortBy(-_.seconds))

final class UserLoginsApi(
    firewall: Firewall,
    store: Store,
    userRepo: UserRepo,
    geoIP: GeoIP,
    ip2proxy: Ip2Proxy,
    printBan: PrintBan
)(using Executor):

  import UserLogins.*

  def apply(user: User, maxOthers: Int): Fu[UserLogins] =
    store.chronoInfoByUser(user) flatMap { infos =>
      val ips = distinctRecent(infos.map(_.datedIp))
      val fps = distinctRecent(infos.flatMap(_.datedFp))
      val fpClients: Map[FingerHash, UserClient] = infos.view.flatMap { i =>
        i.fp map { fp =>
          fp -> UserClient(i.ua)
        }
      }.toMap
      val ipClients: Map[IpAddress, Set[UserClient]] =
        infos.foldLeft(Map.empty[IpAddress, Set[UserClient]]) { (acc, info) =>
          acc.updated(info.ip, acc.get(info.ip).foldLeft(Set(UserClient(info.ua)))(_ ++ _))
        }
      fetchOtherUsers(user, ips.map(_.value).toSet, fps.map(_.value).toSet, maxOthers) zip
        ip2proxy.keepProxies(ips.map(_.value).toList) map { (otherUsers, proxies) =>
          val othersByIp = otherUsers.foldLeft(Map.empty[IpAddress, Set[User]]) { (acc, other) =>
            other.ips.foldLeft(acc) { (acc, ip) =>
              acc.updated(ip, acc.getOrElse(ip, Set.empty) + other.user)
            }
          }
          val othersByFp = otherUsers.foldLeft(Map.empty[FingerHash, Set[User]]) { (acc, other) =>
            other.fps.foldLeft(acc) { (acc, fp) =>
              acc.updated(fp, acc.getOrElse(fp, Set.empty) + other.user)
            }
          }
          UserLogins(
            ips = ips.map { ip =>
              IPData(
                ip,
                firewall blocksIp ip.value,
                geoIP orUnknown ip.value,
                proxies.get(ip.value),
                Alts(othersByIp.getOrElse(ip.value, Set.empty)),
                ipClients.getOrElse(ip.value, Set.empty)
              )
            }.toList,
            prints = fps.map { fp =>
              FPData(
                fp,
                printBan blocks fp.value,
                Alts(othersByFp.getOrElse(fp.value, Set.empty)),
                fpClients.getOrElse(fp.value, UserClient.PC)
              )
            }.toList,
            uas = distinctRecent(infos.map(_.datedUa)).toList,
            otherUsers = otherUsers
          )
        }
    }

  private[security] def userHasPrint(u: User): Fu[Boolean] =
    store.coll.secondaryPreferred.exists($doc("user" -> u.id, "fp" $exists true))

  private def fetchOtherUsers(
      user: User,
      ipSet: Set[IpAddress],
      fpSet: Set[FingerHash],
      max: Int
  ): Fu[List[OtherUser[User]]] =
    ipSet.nonEmpty so store.coll
      .aggregateList(max, readPreference = temporarilyPrimary): framework =>
        import framework.*
        import FingerHash.given
        Match(
          $doc(
            $or(
              "ip" $in ipSet,
              "fp" $in fpSet
            ),
            "user" $ne user.id,
            "date" $gt nowInstant.minusYears(1)
          )
        ) -> List(
          GroupField("user")(
            "ips" -> AddFieldToSet("ip"),
            "fps" -> AddFieldToSet("fp")
          ),
          AddFields(
            $doc(
              "nbIps" -> $doc("$size" -> "$ips"),
              "nbFps" -> $doc("$size" -> "$fps")
            )
          ),
          AddFields(
            $doc(
              "score" -> $doc(
                "$add" -> $arr("$nbIps", "$nbFps", $doc("$multiply" -> $arr("$nbIps", "$nbFps")))
              )
            )
          ),
          Sort(Descending("score")),
          Limit(max),
          PipelineOperator(
            $doc(
              "$lookup" -> $doc(
                "from"         -> userRepo.coll.name,
                "localField"   -> "_id",
                "foreignField" -> "_id",
                "as"           -> "user"
              )
            )
          ),
          UnwindField("user")
        )
      .map: docs =>
        import lila.user.User.given
        import FingerHash.given
        for
          doc  <- docs
          user <- doc.getAsOpt[User]("user")
          ips  <- doc.getAsOpt[Set[IpAddress]]("ips")(collectionReader)
          fps  <- doc.getAsOpt[Set[FingerHash]]("fps")(collectionReader)
        yield OtherUser(user, ips intersect ipSet, fps intersect fpSet)

  def getUserIdsWithSameIpAndPrint(userId: UserId): Fu[Set[UserId]] =
    for
      (ips, fps) <- nextValues("ip", userId) zip nextValues("fp", userId)
      users <- (ips.nonEmpty && fps.nonEmpty) so store.coll.secondaryPreferred.distinctEasy[UserId, Set](
        "user",
        $doc(
          "ip" $in ips,
          "fp" $in fps,
          "user" $ne userId
        )
      )
    yield users

  private def nextValues(field: String, userId: UserId): Fu[Set[String]] =
    store.coll.secondaryPreferred.distinctEasy[String, Set](field, $doc("user" -> userId))

object UserLogins:

  case class OtherUser[U](user: U, ips: Set[IpAddress], fps: Set[FingerHash])(using userIdOf: UserIdOf[U]):
    val nbIps  = ips.size
    val nbFps  = fps.size
    val score  = nbIps + nbFps + nbIps * nbFps
    def userId = userIdOf(user)

  // distinct values, keeping the most recent of duplicated values
  // assumes all is sorted by most recent first
  def distinctRecent[V](all: List[Dated[V]]): scala.collection.View[Dated[V]] =
    all
      .foldLeft(Map.empty[V, Instant]) {
        case (acc, Dated(v, _)) if acc.contains(v) => acc
        case (acc, Dated(v, date))                 => acc + (v -> date)
      }
      .view
      .map(Dated.apply)

  case class Alts(users: Set[User]):
    lazy val boosters = users.count(_.marks.boost)
    lazy val engines  = users.count(_.marks.engine)
    lazy val trolls   = users.count(_.marks.troll)
    lazy val alts     = users.count(_.marks.alt)
    lazy val closed   = users.count(u => u.enabled.no && u.marks.clean)
    lazy val cleans   = users.count(u => u.enabled.yes && u.marks.clean)
    def score =
      (boosters * 10 + engines * 10 + trolls * 10 + alts * 10 + closed * 2 + cleans) match
        case 0 => -999999 // rank empty alts last
        case n => n

  case class IPData(
      ip: Dated[IpAddress],
      blocked: Boolean,
      location: Location,
      proxy: Option[String],
      alts: Alts,
      clients: Set[UserClient]
  ):
    def datedLocation = Dated(Location.WithProxy(location, proxy), ip.date)

  case class FPData(
      fp: Dated[FingerHash],
      banned: Boolean,
      alts: Alts,
      client: UserClient
  )

  case class WithMeSortedWithEmails[U: UserIdOf](
      others: List[OtherUser[U]],
      emails: Map[UserId, EmailAddress]
  ) {
    def withUsers[V: UserIdOf](newUsers: List[V]) = copy(others = others.flatMap { o =>
      newUsers.find(_ is o.user).map { u => o.copy(user = u) }
    })
  }

  def withMeSortedWithEmails(
      userRepo: UserRepo,
      me: User,
      userLogins: UserLogins
  )(using Executor): Fu[WithMeSortedWithEmails[User]] =
    userRepo.emailMap(me.id :: userLogins.otherUsers.map(_.user.id)) map { emailMap =>
      WithMeSortedWithEmails(
        OtherUser(me, userLogins.rawIps.toSet, userLogins.rawFps.toSet) :: userLogins.otherUsers,
        emailMap
      )
    }

  case class TableData[U](
      userLogins: UserLogins,
      othersWithEmail: UserLogins.WithMeSortedWithEmails[U],
      notes: List[lila.user.Note],
      bans: Map[UserId, Int],
      max: Int
  ) {
    def withUsers[V: UserIdOf](users: List[V]) = copy(
      othersWithEmail = othersWithEmail.withUsers(users)
    )
  }
