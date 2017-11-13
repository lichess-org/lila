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
      val others = spy.usersSharingFingerprint.toList
        .sortBy(-_.createdAt.getSeconds)
        .takeWhile(_.createdAt.isAfter(DateTime.now minusDays 3))
        .take(5)
      (others.size > 2 && others.forall(closedSB)) ?? {
        ipIntel(ip).map { 75 < _ }.map {
          _ ?? {
            val ipBan = spy.usersSharingIp.forall { u =>
              closedSB(u) || !u.seenAt.exists(DateTime.now.minusMonths(2).isBefore)
            }
            collect(user, email, others, ipBan)
          }
        }
      }
    }

  private def closedSB(user: User) =
    (user.troll || user.engine) && !user.enabled

  private val emailSuffixes = "yandex.ru yandex.com mailfa.com"
    .split(' ').toList.map("@" + _)

  private def checkable(user: User, email: EmailAddress): Boolean =
    user.createdAt.isAfter(DateTime.now minusDays 3) &&
      emailSuffixes.exists(email.value.endsWith)

  private def isEffective =
    configColl.primitiveOne[Boolean]($id("ugc"), "value").map(~_)

  private def collect(user: User, email: EmailAddress, others: List[User], ipBan: Boolean): Funit = isEffective flatMap { effective =>
    val wait = (10 + scala.util.Random.nextInt(120)).seconds
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
    slack.garbageCollector(s"@${user.username} has been dealt with.")
  }
}
