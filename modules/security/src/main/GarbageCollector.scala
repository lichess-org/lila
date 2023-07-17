package lila.security

import play.api.mvc.RequestHeader
import ornicar.scalalib.ThreadLocalRandom

import lila.common.{ Bus, EmailAddress, HTTPRequest, IpAddress }
import lila.user.User

// codename UGC
final class GarbageCollector(
    userLogins: UserLoginsApi,
    ipTrust: IpTrust,
    irc: lila.irc.IrcApi,
    noteApi: lila.user.NoteApi,
    isArmed: () => Boolean
)(using
    ec: Executor,
    scheduler: Scheduler
):

  private val logger = lila.security.logger.branch("GarbageCollector")

  private val justOnce = lila.memo.OnceEvery[UserId](10 minutes)

  private case class ApplyData(
      user: User,
      ip: IpAddress,
      email: EmailAddress,
      req: RequestHeader,
      quickly: Boolean
  ):
    override def toString = s"${user.username} $ip ${email.value} $req"

  // User just signed up and doesn't have security data yet, so wait a bit
  def delay(user: User, email: EmailAddress, req: RequestHeader, quickly: Boolean): Unit =
    if user.createdAt.isAfter(nowInstant minusDays 3) then
      val ip = HTTPRequest ipAddress req
      scheduler.scheduleOnce(6 seconds):
        val applyData = ApplyData(user, ip, email, req, quickly)
        logger.debug(s"delay $applyData")
        lila.common.LilaFuture
          .retry(
            () => ensurePrintAvailable(applyData),
            delay = 10 seconds,
            retries = 5,
            logger = none
          )
          .recoverDefault >> apply(applyData)

  private def ensurePrintAvailable(data: ApplyData): Funit =
    userLogins userHasPrint data.user flatMap {
      case false => fufail("No print available yet")
      case _     => funit
    }

  private def apply(data: ApplyData): Funit =
    data match
      case ApplyData(user, ip, email, req, quickly) =>
        for
          spy    <- userLogins(user, 300)
          ipSusp <- ipTrust.isSuspicious(ip)
          _ <-
            val printOpt = spy.prints.headOption
            logger.debug(s"apply ${data.user.username} print=$printOpt")
            Bus.publish(
              lila.security.UserSignup(user, email, req, printOpt.map(_.fp.value), ipSusp),
              "userSignup"
            )
            printOpt.filter(_.banned).map(_.fp.value) match
              case Some(print) =>
                collect(user, email, msg = s"Print ban: `${print.value}`", quickly = quickly)
              case _ =>
                badOtherAccounts(spy.otherUsers.map(_.user)).so: others =>
                  logger.debug(s"other ${data.user.username} others=${others.map(_.username)}")
                  spy.ips
                    .existsM(ipTrust.isSuspicious)
                    .map:
                      _ so collect(
                        user,
                        email,
                        msg = s"Prev users: ${others.map(o => "@" + o.username).mkString(", ")}",
                        quickly = quickly
                      )
        yield ()

  private def badOtherAccounts(accounts: List[User]): Option[List[User]] =
    val others = accounts
      .sortBy(-_.createdAt.toMillis)
      .takeWhile(_.createdAt.isAfter(nowInstant minusDays 10))
      .take(4)
    (others.sizeIs > 1 && others.forall(isBadAccount) && others.headOption.exists(_.enabled.no)) option others

  private def isBadAccount(user: User) = user.lameOrTrollOrAlt

  private def collect(user: User, email: EmailAddress, msg: => String, quickly: Boolean): Funit =
    justOnce(user.id).so:
      !hasBeenCollectedBefore(user) map {
        if _ then
          val armed = isArmed()
          val wait  = if quickly then 3.seconds else (30 + ThreadLocalRandom.nextInt(300)).seconds
          val message =
            s"Will dispose of https://lichess.org/${user.username} in $wait. Email: ${email.value}. $msg${!armed so " [SIMULATION]"}"
          logger.info(message)
          noteApi.lichessWrite(user, s"Garbage collected because of $msg")
          if armed then
            scheduler.scheduleOnce(wait):
              doCollect(user.id)
      }

  private def hasBeenCollectedBefore(user: User): Fu[Boolean] =
    noteApi.byUserForMod(user.id).map(_.exists(_.text startsWith "Garbage collected"))

  private def doCollect(user: UserId): Unit =
    Bus.publish(
      lila.hub.actorApi.security.GarbageCollect(user),
      "garbageCollect"
    )
