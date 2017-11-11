package lila.security

import org.joda.time.DateTime

import lila.common.{ EmailAddress, IpAddress }
import lila.user.{ User, UserRepo }

final class AutoKill(
    userSpy: UserSpyApi,
    ipIntel: IpIntel,
    slack: lila.slack.SlackApi,
    system: akka.actor.ActorSystem
) {

  def apply(user: User, ip: IpAddress, email: EmailAddress): Funit = checkable(user, email) ?? {
    userSpy(user) flatMap { spy =>
      val others = spy.usersSharingFingerprint
        .sortBy(-_.createdAt.getSeconds)
        .takeWhile(_.createdAt.isAfter(DateTime.now minusDays 3))
        .take(5)
      (others.size > 2 && others.forall(killed)) ??
        ipIntel(ip).map { 75 < _ }
    }
  } map {
    _ ?? kill(user)
  }

  private def killed(user: User) =
    user.troll && user.engine && !user.enabled

  private def checkable(user: User, email: EmailAddress) =
    email.value.endsWith("@yandex.ru") ||
      email.value.endsWith("@yandex.com")

  private def kill(user: User): Funit = {
    logger.info(s"Autokill ${user.username}")
    slack.autoKill(user)
    // just log for now.
  }
}
