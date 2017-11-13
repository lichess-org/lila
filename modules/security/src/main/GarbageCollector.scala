package lila.security

import org.joda.time.DateTime
import scala.concurrent.duration._

import lila.common.{ EmailAddress, IpAddress }
import lila.user.{ User, UserRepo }

final class GarbageCollector(
    userSpy: UserSpyApi,
    ipIntel: IpIntel,
    slack: lila.slack.SlackApi,
    system: akka.actor.ActorSystem
) {

  private val effective = false

  /* User just signed up and doesn't have security data yet,
   * so wait a bit */
  def delay(user: User, ip: IpAddress, email: EmailAddress): Unit =
    if (checkable(email).pp("checkable")) system.scheduler.scheduleOnce(5 seconds) {
      apply(user, ip, email)
    }

  private def apply(user: User, ip: IpAddress, email: EmailAddress): Funit =
    userSpy(user).thenPp flatMap { spy =>
      val others = spy.usersSharingFingerprint.toList
        .sortBy(-_.createdAt.getSeconds)
        .takeWhile(_.createdAt.isAfter(DateTime.now minusDays 3))
        .take(5).pp
      (others.size > 2 && others.forall(closedSB)) ?? {
        ipIntel(ip).map { 75 < _ }.map {
          _ ?? {
            collect(user, email, others)
          }
        }
      }
    }

  private def closedSB(user: User) =
    (user.troll || user.engine) && !user.enabled

  private def checkable(email: EmailAddress) =
    email.value.endsWith("@yandex.ru") ||
      email.value.endsWith("@yandex.com")

  private def collect(user: User, email: EmailAddress, others: List[User]): Funit = {
    val wait = (20 + scala.util.Random.nextInt(200)).seconds
    val othersStr = others.map(o => "@" + o.username).mkString(", ")
    val message = s"Will garbage collect @${user.username} in $wait. $email ($othersStr)${!effective ?? " [SIMULATION]"}"
    logger.info(message)
    slack.garbageCollector(user) >>- {
      effective ?? doCollect(user)
    }
  }

  private def doCollect(user: User): Unit =
    system.lilaBus.publish(
      lila.hub.actorApi.security.GarbageCollect(user.id),
      'garbageCollect
    )
}
