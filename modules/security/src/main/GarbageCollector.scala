package lila.security

import play.api.mvc.RequestHeader
import scalalib.ThreadLocalRandom

import lila.common.{ Bus, HTTPRequest }
import lila.core.lilaism.LilaNoStackTrace
import lila.core.net.IpAddress
import lila.core.security.UserSignup
import lila.common.LilaFuture
import lila.core.data.LazyDep

// codename UGC
final class GarbageCollector(
    userLogins: UserLoginsApi,
    ipTrust: IpTrust,
    noteApi: lila.user.NoteApi,
    currentlyPlaying: LazyDep[lila.core.round.CurrentlyPlaying],
    isArmed: () => Boolean
)(using ec: Executor, scheduler: Scheduler):

  private val logger = lila.security.logger.branch("GarbageCollector")

  private val justOnce = scalalib.cache.OnceEvery[UserId](20.minutes)

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
    if user.createdAt.isAfter(nowInstant.minusDays(3)) then
      val ip = HTTPRequest.ipAddress(req)
      scheduler.scheduleOnce(6.seconds):
        val applyData = ApplyData(user, ip, email, req, quickly)
        logger.debug(s"delay $applyData")
        lila.common.LilaFuture
          .retry(
            () => ensurePrintAvailable(applyData),
            delay = 10.seconds,
            retries = 5,
            logger = none
          )
          .recoverDefault(e => logger.info(e.getMessage, e)) >> apply(applyData)

  private def ensurePrintAvailable(data: ApplyData): Funit =
    userLogins
      .userHasPrint(data.user)
      .flatMap:
        case false => fufail(LilaNoStackTrace(s"never got a print for ${data.user.username}"))
        case _     => funit

  private def apply(data: ApplyData): Funit =
    import data.*
    for
      spy    <- userLogins(user, 300)
      ipSusp <- ipTrust.isSuspicious(ip)
      _ <-
        val printOpt = spy.prints.headOption
        logger.debug(s"apply ${data.user.username} print=$printOpt")
        Bus.publish(
          UserSignup(user, email, req, printOpt.map(_.fp.value), ipSusp),
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
                .mapz:
                  collect(
                    user,
                    email,
                    msg = s"Prev users: ${others.map(o => "@" + o.username).mkString(", ")}",
                    quickly = quickly
                  )
    yield ()

  private def badOtherAccounts(accounts: List[User]): Option[List[User]] =
    val others = accounts
      .sortBy(-_.createdAt.toMillis)
      .takeWhile(_.createdAt.isAfter(nowInstant.minusDays(10)))
      .take(4)
    (others.sizeIs > 1 && others.forall(isBadAccount) && others.headOption.exists(_.enabled.no))
      .option(others)

  private def isBadAccount(u: User) = u.lameOrTroll || u.marks.alt

  private def collect(user: User, email: EmailAddress, msg: => String, quickly: Boolean): Funit =
    justOnce(user.id).so:
      hasBeenCollectedBefore(user).not.mapz:
        val armed = isArmed()
        val wait  = if quickly then 3.seconds else (60 * 5 + ThreadLocalRandom.nextInt(60 * 10)).seconds
        logger.info:
          s"Will dispose of https://lichess.org/${user.username} in $wait. Email: ${email.value}. $msg${(!armed).so(" [DRY]")}"
        noteApi.lichessWrite(user, s"Garbage collection in $wait because of $msg")
        if armed then
          for _ <- waitForCollection(user.id, nowInstant.plus(wait))
          do Bus.publish(lila.core.security.GarbageCollect(user.id), "garbageCollect")

  private def hasBeenCollectedBefore(user: User): Fu[Boolean] =
    noteApi.toUserForMod(user.id).map(_.exists(_.text.startsWith("Garbage collect")))

  private def waitForCollection(userId: UserId, max: Instant): Funit =
    if nowInstant.isAfter(max) then funit
    else
      currentlyPlaying
        .resolve()
        .exec(userId)
        .map2(_.game)
        .flatMap: game =>
          if game.exists(_.playedTurns > 25) then funit
          else
            LilaFuture.delay(if game.isDefined then 10.seconds else 30.seconds):
              waitForCollection(userId, max)
