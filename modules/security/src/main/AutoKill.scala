package lila.security

import org.joda.time.DateTime
import scala.concurrent.duration._

import lila.common.{ EmailAddress, IpAddress }
import lila.user.{ User, UserRepo }

final class AutoKill(
    userSpy: UserSpyApi,
    ipIntel: IpIntel,
    slack: lila.slack.SlackApi,
    system: akka.actor.ActorSystem
) {

  /* User just signed up and doesn't have security data yet,
   * so wait a bit */
  def delay(user: User, ip: IpAddress, email: EmailAddress): Unit =
    if (checkable(email)) system.scheduler.scheduleOnce(5 seconds) {
      apply(user, ip, email)
    }

  private def apply(user: User, ip: IpAddress, email: EmailAddress): Funit =
    userSpy(user) flatMap { spy =>
      val others = spy.usersSharingFingerprint
        .sortBy(-_.createdAt.getSeconds)
        .takeWhile(_.createdAt.isAfter(DateTime.now minusDays 3))
        .take(5)
      (others.size > 2 && others.forall(killed)) ??
        ipIntel(ip).map { 75 < _ }
    } map {
      _ ?? kill(user)
    }

  private def killed(user: User) =
    user.troll && !user.enabled

  private def checkable(email: EmailAddress) =
    email.value.endsWith("@yandex.ru") ||
      email.value.endsWith("@yandex.com")

  private def kill(user: User): Funit = {
    logger.info(s"Autokill ${user.username}")
    slack.autoKill(user)
    // just log for now.
  }
}
