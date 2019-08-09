package lidraughts.security

import org.joda.time.DateTime
import play.api.mvc.RequestHeader
import scala.concurrent.duration._

import lidraughts.common.{ EmailAddress, IpAddress, HTTPRequest }
import lidraughts.user.{ User, UserRepo }

// codename UGC
final class GarbageCollector(
    userSpy: UserSpyApi,
    ipTrust: IpTrust,
    printBan: PrintBan,
    slack: lidraughts.slack.SlackApi,
    isArmed: () => Boolean,
    system: akka.actor.ActorSystem
) {

  private val logger = lidraughts.security.logger.branch("GarbageCollector")

  private val done = new lidraughts.memo.ExpireSetMemo(10 minutes)

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
        lidraughts.common.Future.retry(
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
        val printOpt = spy.prints.headOption
        logger.debug(s"apply ${data.user.username} print=${printOpt}")
        system.lidraughtsBus.publish(
          lidraughts.security.Signup(user, email, req, printOpt.map(_.value)),
          'userSignup
        )
        printOpt.map(_.value) filter printBan.blocks match {
          case Some(print) => collect(user, email, ipBan = false, msg = s"Print ban: ${print.value}")
          case _ =>
            badOtherAccounts(spy.otherUsers.map(_.user)) ?? { others =>
              logger.debug(s"other ${data.user.username} others=${others.map(_.username)}")
              lidraughts.common.Future.exists(spy.ips)(ipTrust.isSuspicious).map {
                _ ?? collect(user, email,
                  ipBan = spy.usersSharingIp.forall { u =>
                    isBadAccount(u) || !u.seenAt.exists(DateTime.now.minusMonths(2).isBefore)
                  },
                  msg = s"Prev users: ${others.map(o => "@" + o.username).mkString(", ")}")
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

  private def collect(user: User, email: EmailAddress, ipBan: Boolean, msg: => String): Funit = !done.get(user.id) ?? {
    done put user.id
    val armed = isArmed()
    val wait = (30 + scala.util.Random.nextInt(300)).seconds
    val message = s"Will dispose of @${user.username} in $wait. Email: ${email.value}. $msg${!armed ?? " [SIMULATION]"}"
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
    system.lidraughtsBus.publish(
      lidraughts.hub.actorApi.security.GCImmediateSb(user.id),
      'garbageCollect
    )

  private def doCollect(user: User, ipBan: Boolean): Unit =
    system.lidraughtsBus.publish(
      lidraughts.hub.actorApi.security.GarbageCollect(user.id, ipBan),
      'garbageCollect
    )
}
