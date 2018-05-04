package lila.security

import org.joda.time.DateTime
import scala.concurrent.duration._

import lila.common.{ EmailAddress, IpAddress }
import lila.user.{ User, UserRepo }

// codename UGC
final class GarbageCollector(
    userSpy: UserSpyApi,
    ipTrust: IpTrust,
    slack: lila.slack.SlackApi,
    isArmed: () => Boolean,
    system: akka.actor.ActorSystem
) {

  // User just signed up and doesn't have security data yet, so wait a bit
  def delay(user: User, ip: IpAddress, email: EmailAddress): Unit =
    if (user.createdAt.isAfter(DateTime.now minusDays 3)) {
      debug(email, s"${user.username} $email $ip", "pre")
      system.scheduler.scheduleOnce(1 minute) {
        apply(user, ip, email)
      }
    }

  private def apply(user: User, ip: IpAddress, email: EmailAddress): Funit =
    userSpy(user) flatMap { spy =>
      debug(email, spy, s"spy ${user.username}")
      badOtherAccounts(spy.otherUsers.map(_.user)) ?? { others =>
        debug(email, others.map(_.id), s"others ${user.username}")
        lila.common.Future.exists(spy.ips)(ipTrust.isSuspicious).map {
          _ ?? {
            val ipBan = spy.usersSharingIp.forall { u =>
              isBadAccount(u) || !u.seenAt.exists(DateTime.now.minusMonths(2).isBefore)
            }
            collect(user, email, others, ipBan)
          }
        }
      }
    }

  private def debug(email: EmailAddress, stuff: Any, as: String = "-") =
    if (email.value contains "iralas".reverse) logger.info(s"GC debug $as: $stuff")

  private def badOtherAccounts(accounts: Set[User]): Option[List[User]] = {
    val others = accounts.toList
      .sortBy(-_.createdAt.getSeconds)
      .takeWhile(_.createdAt.isAfter(DateTime.now minusDays 10))
      .take(4)
    (others.size > 1 && others.forall(isBadAccount) && others.headOption.exists(_.disabled)) option others
  }

  private def isBadAccount(user: User) = user.troll || user.engine

  private def collect(user: User, email: EmailAddress, others: List[User], ipBan: Boolean): Funit = {
    val armed = isArmed()
    val wait = (30 + scala.util.Random.nextInt(300)).seconds
    val othersStr = others.map(o => "@" + o.username).mkString(", ")
    val message = s"Will dispose of @${user.username} in $wait. Email: $email. Prev users: $othersStr${!armed ?? " [SIMULATION]"}"
    logger.branch("GarbageCollector").info(message)
    slack.garbageCollector(message) >>- {
      if (armed) system.scheduler.scheduleOnce(wait) {
        doCollect(user, ipBan)
      }
    }
  }

  private def doCollect(user: User, ipBan: Boolean): Unit =
    system.lilaBus.publish(
      lila.hub.actorApi.security.GarbageCollect(user.id, ipBan),
      'garbageCollect
    )
}
