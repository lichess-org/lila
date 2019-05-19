package lila.security

import org.joda.time.DateTime
import play.api.mvc.RequestHeader
import scala.concurrent.duration._

import lila.common.{ EmailAddress, IpAddress, HTTPRequest }
import lila.user.{ User, UserRepo }

// codename UGC
final class GarbageCollector(
    userSpy: UserSpyApi,
    ipTrust: IpTrust,
    slack: lila.slack.SlackApi,
    isArmed: () => Boolean,
    system: akka.actor.ActorSystem
) {

  private val logger = lila.security.logger.branch("GarbageCollector")

  private val done = new lila.memo.ExpireSetMemo(10 minutes)

  private case class ApplyData(user: User, ip: IpAddress, email: EmailAddress, req: RequestHeader) {
    override def toString = s"${user.username} $ip ${email.value} $req"
  }

  // User just signed up and doesn't have security data yet, so wait a bit
  def delay(user: User, email: EmailAddress, req: RequestHeader): Unit =
    if (user.createdAt.isAfter(DateTime.now minusDays 3)) {
      val ip = HTTPRequest lastRemoteAddress req
      system.scheduler.scheduleOnce(6 seconds) {
        val applyData = ApplyData(user, ip, email, req)
        logger.debug(s"delay $applyData")
        lila.common.Future.retry(
          () => ensurePrintAvailable(applyData),
          delay = 10 seconds,
          retries = 5,
          logger = none
        )(system).nevermind >> apply(applyData)
      }
    }

  private def ensurePrintAvailable(data: ApplyData): Funit =
    userSpy userHasPrint data.user flatMap {
      case false => fufail("No print available yet")
      case _ => funit
    }

  private def apply(data: ApplyData): Funit = data match {
    case ApplyData(user, ip, email, req) =>
      userSpy(user) flatMap { spy =>
        val print = spy.prints.headOption
        logger.debug(s"apply ${data.user.username} print=${print}")
        system.lilaBus.publish(
          lila.security.Signup(user, email, req, print.map(_.value)),
          'userSignup
        )
        badOtherAccounts(spy.otherUsers.map(_.user)) ?? { others =>
          logger.debug(s"other ${data.user.username} others=${others.map(_.username)}")
          lila.common.Future.exists(spy.ips)(ipTrust.isSuspicious).map {
            _ ?? {
              val ipBan = spy.usersSharingIp.forall { u =>
                isBadAccount(u) || !u.seenAt.exists(DateTime.now.minusMonths(2).isBefore)
              }
              if (!done.get(user.id)) {
                collect(user, email, others, ipBan)
                done put user.id
              }
            }
          }
        }
      }
  }

  private def badOtherAccounts(accounts: Set[User]): Option[List[User]] = {
    val others = accounts.toList
      .sortBy(-_.createdAt.getSeconds)
      .takeWhile(_.createdAt.isAfter(DateTime.now minusDays 10))
      .take(4)
    (others.size > 1 && others.forall(isBadAccount) && others.headOption.exists(_.disabled)) option others
  }

  private def isBadAccount(user: User) = user.lameOrTroll

  private def collect(user: User, email: EmailAddress, others: List[User], ipBan: Boolean): Funit = {
    val armed = isArmed()
    val wait = (30 + scala.util.Random.nextInt(300)).seconds
    val othersStr = others.map(o => "@" + o.username).mkString(", ")
    val message = s"Will dispose of @${user.username} in $wait. Email: ${email.value}. Prev users: $othersStr${!armed ?? " [SIMULATION]"}"
    logger.info(message)
    slack.garbageCollector(message) >>- {
      if (armed) {
        doInitialSb(user)
        system.scheduler.scheduleOnce(wait) {
          doCollect(user, ipBan)
        }
      }
    }
  }

  private def doInitialSb(user: User): Unit =
    system.lilaBus.publish(
      lila.hub.actorApi.security.GCImmediateSb(user.id),
      'garbageCollect
    )

  private def doCollect(user: User, ipBan: Boolean): Unit =
    system.lilaBus.publish(
      lila.hub.actorApi.security.GarbageCollect(user.id, ipBan),
      'garbageCollect
    )
}
