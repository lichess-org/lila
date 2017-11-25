package lila.security

import org.joda.time.DateTime
import reactivemongo.bson._
import scala.concurrent.duration._

import lila.common.{ EmailAddress, IpAddress }
import lila.db.dsl._
import lila.user.{ User, UserRepo }

// codename UGC
final class GarbageCollector(
    userSpy: UserSpyApi,
    ipIntel: IpIntel,
    slack: lila.slack.SlackApi,
    configColl: Coll,
    system: akka.actor.ActorSystem
) {

  /* User just signed up and doesn't have security data yet,
   * so wait a bit */
  def delay(user: User, ip: IpAddress, email: EmailAddress): Unit =
    if (checkable(user, email)) system.scheduler.scheduleOnce(5 seconds) {
      apply(user, ip, email)
    }

  private def apply(user: User, ip: IpAddress, email: EmailAddress): Funit =
    userSpy(user) flatMap { spy =>
      badOtherAccounts(spy.otherUsers.map(_.user)) ?? { others =>
        lila.common.Future.exists(spy.ips)(isBadIp).map {
          _ ?? {
            val ipBan = spy.usersSharingIp.forall { u =>
              isBadAccount(u) || !u.seenAt.exists(DateTime.now.minusMonths(2).isBefore)
            }
            collect(user, email, others, ipBan)
          }
        }
      }
    }

  private def badOtherAccounts(accounts: Set[User]): Option[List[User]] = {
    val others = accounts.toList
      .sortBy(-_.createdAt.getSeconds)
      .takeWhile(_.createdAt.isAfter(DateTime.now minusDays 7))
      .take(4)
    (others.size > 1 && others.forall(isBadAccount)) option others
  }

  private def isBadIp(ip: UserSpy.IPData): Fu[Boolean] = if (ip.blocked ||
    ip.location == Location.unknown ||
    ip.location == Location.tor ||
    ip.location.shortCountry == "Iran" // some undetected proxies
    ) fuccess(true)
  else ipIntel(ip.ip).map { 75 < _ }

  private def isBadAccount(user: User) =
    (user.troll || user.engine) && !user.enabled

  private val emailSuffixes = "yandex.ru yandex.com mailfa.com juno.com"
    .split(' ').toList.map("@" + _)

  private def checkable(user: User, email: EmailAddress): Boolean =
    user.createdAt.isAfter(DateTime.now minusDays 3) &&
      emailSuffixes.exists(email.value.endsWith)

  private def isEffective =
    configColl.primitiveOne[Boolean]($id("ugc"), "value").map(~_)

  private def collect(user: User, email: EmailAddress, others: List[User], ipBan: Boolean): Funit = isEffective flatMap { effective =>
    val wait = (30 + scala.util.Random.nextInt(300)).seconds
    val othersStr = others.map(o => "@" + o.username).mkString(", ")
    val message = s"Will dispose of @${user.username} in $wait. Email: $email. Prev users: $othersStr${!effective ?? " [SIMULATION]"}"
    logger.branch("GarbageCollector").info(message)
    slack.garbageCollector(message) >>- {
      if (effective) system.scheduler.scheduleOnce(wait) {
        doCollect(user, ipBan)
      }
    }
  }

  private def doCollect(user: User, ipBan: Boolean): Unit = {
    system.lilaBus.publish(
      lila.hub.actorApi.security.GarbageCollect(user.id, ipBan),
      'garbageCollect
    )
  }
}
