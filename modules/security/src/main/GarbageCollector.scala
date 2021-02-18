package lila.security

import org.joda.time.DateTime
import play.api.mvc.RequestHeader
import scala.concurrent.duration._

import lila.common.{ Bus, EmailAddress, HTTPRequest, IpAddress, ThreadLocalRandom }
import lila.user.User

// codename UGC
final class GarbageCollector(
    userLogins: UserLoginsApi,
    ipTrust: IpTrust,
    slack: lila.irc.SlackApi,
    noteApi: lila.user.NoteApi,
    isArmed: () => Boolean
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: akka.actor.ActorSystem
) {

  private val logger = lila.security.logger.branch("GarbageCollector")

  private val justOnce = lila.memo.OnceEvery(10 minutes)

  private case class ApplyData(user: User, ip: IpAddress, email: EmailAddress, req: RequestHeader) {
    override def toString = s"${user.username} $ip ${email.value} $req"
  }

  // User just signed up and doesn't have security data yet, so wait a bit
  def delay(user: User, email: EmailAddress, req: RequestHeader): Unit =
    if (user.createdAt.isAfter(DateTime.now minusDays 3)) {
      val ip = HTTPRequest ipAddress req
      system.scheduler
        .scheduleOnce(6 seconds) {
          val applyData = ApplyData(user, ip, email, req)
          logger.debug(s"delay $applyData")
          lila.common.Future
            .retry(
              () => ensurePrintAvailable(applyData),
              delay = 10 seconds,
              retries = 5,
              logger = none
            )
            .nevermind >> apply(applyData)
          ()
        }
        .unit
    }

  private def ensurePrintAvailable(data: ApplyData): Funit =
    userLogins userHasPrint data.user flatMap {
      case false => fufail("No print available yet")
      case _     => funit
    }

  private def apply(data: ApplyData): Funit =
    data match {
      case ApplyData(user, ip, email, req) =>
        for {
          spy    <- userLogins(user, 300)
          ipSusp <- ipTrust.isSuspicious(ip)
          _ <- {
            val printOpt = spy.prints.headOption
            logger.debug(s"apply ${data.user.username} print=$printOpt")
            Bus.publish(
              lila.security.UserSignup(user, email, req, printOpt.map(_.fp.value), ipSusp),
              "userSignup"
            )
            printOpt.filter(_.banned).map(_.fp.value) match {
              case Some(print) => collect(user, email, msg = s"Print ban: ${print.value}")
              case _ =>
                badOtherAccounts(spy.otherUsers.map(_.user)) ?? { others =>
                  logger.debug(s"other ${data.user.username} others=${others.map(_.username)}")
                  lila.common.Future
                    .exists(spy.ips)(ipTrust.isSuspicious)
                    .map {
                      _ ?? collect(
                        user,
                        email,
                        msg = s"Prev users: ${others.map(o => "@" + o.username).mkString(", ")}"
                      )
                    }
                }
            }
          }
        } yield ()
    }

  private def badOtherAccounts(accounts: List[User]): Option[List[User]] = {
    val others = accounts
      .sortBy(-_.createdAt.getSeconds)
      .takeWhile(_.createdAt.isAfter(DateTime.now minusDays 10))
      .take(4)
    (others.sizeIs > 1 && others.forall(isBadAccount) && others.headOption.exists(_.disabled)) option others
  }

  private def isBadAccount(user: User) = user.lameOrTrollOrAlt

  private def collect(user: User, email: EmailAddress, msg: => String): Funit =
    justOnce(user.id) ?? {
      val armed = isArmed()
      val wait  = (30 + ThreadLocalRandom.nextInt(300)).seconds
      val message =
        s"Will dispose of @${user.username} in $wait. Email: ${email.value}. $msg${!armed ?? " [SIMULATION]"}"
      logger.info(message)
      noteApi.lichessWrite(user, s"Garbage collected because of $msg")
      slack.garbageCollector(message) >>- {
        if (armed) {
          doInitialSb(user)
          system.scheduler
            .scheduleOnce(wait) {
              doCollect(user)
            }
            .unit
        }
      }
    }

  private def doInitialSb(user: User): Unit =
    Bus.publish(
      lila.hub.actorApi.security.GCImmediateSb(user.id),
      "garbageCollect"
    )

  private def doCollect(user: User): Unit =
    Bus.publish(
      lila.hub.actorApi.security.GarbageCollect(user.id),
      "garbageCollect"
    )
}
