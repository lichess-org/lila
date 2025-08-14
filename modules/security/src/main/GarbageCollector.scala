package lila.security

import play.api.mvc.RequestHeader
import scalalib.ThreadLocalRandom

import lila.common.{ Bus, HTTPRequest }
import lila.core.lilaism.LilaNoStackTrace
import lila.core.net.IpAddress
import lila.core.security.UserSignup
import lila.common.LilaFuture
import lila.core.data.LazyDep

final class GarbageCollector(
    userLogins: UserLoginsApi,
    ipTrust: IpTrust,
    noteApi: lila.user.NoteApi,
    currentlyPlaying: LazyDep[lila.core.round.CurrentlyPlaying],
    isArmed: () => Boolean,
    userRepo: lila.user.UserRepo
)(using ec: Executor, scheduler: Scheduler):

  private val logger = lila.security.logger.branch("GarbageCollector")

  private val justOnce = scalalib.cache.OnceEvery[UserId](1.hour)

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
        case _ => funit

  private def apply(data: ApplyData): Funit =
    import data.*
    for
      alts <- userLogins(user, 300)
      ipSusp <- ipTrust.isSuspicious(ip)
      _ <-
        val printOpt = alts.prints.headOption
        logger.debug(s"apply ${data.user.username} print=$printOpt")
        Bus.pub(UserSignup(user, email, req, printOpt.map(_.fp.value), ipSusp))
        printOpt.filter(_.banned).map(_.fp.value) match
          case Some(print) =>
            waitThenCollect(user, msg = s"Print ban: `$print`", quickly = quickly)
          case _ =>
            allRecentAltsAreMarked(alts).so: markedAlts =>
              val msg = s"Prev marked users: ${markedAlts.map(o => "@" + o.username).mkString(", ")}"
              waitThenCollect(user, msg = msg, quickly = quickly)
    yield ()

  private def allRecentAltsAreMarked(alts: UserLogins): Option[List[User]] =
    val minBadRecentAlts = 5
    val latestAlts = alts.otherUsers.sortByReverse(_.atInstant).take(minBadRecentAlts).map(_.user)
    val found = latestAlts.size == minBadRecentAlts &&
      latestAlts.forall(isBadAccount) &&
      latestAlts.forall(_.atInstant.isAfter(nowInstant.minusDays(7)))
    found.option(latestAlts)

  private def isBadAccount(u: User) = u.lameOrTroll || u.marks.alt

  def waitThenCollect(user: User, msg: => String, quickly: Boolean): Funit =
    justOnce(user.id).so:
      hasBeenCollectedBefore(user).not.mapz:
        val armed = isArmed()
        val wait = if quickly then 3.seconds else (10 + ThreadLocalRandom.nextInt(30)).minutes
        logger.info:
          s"Will dispose of https://lichess.org/${user.username} in $wait. $msg${(!armed).so(" [DRY]")}"
        noteApi.lichessWrite(user, s"Garbage collection in $wait because of $msg")
        if armed then
          for
            _ <- userRepo.setAlt(user.id, true)
            _ <- waitForCollection(user.id, nowInstant.plus(wait))
            stillAlted <- userRepo.isAlt(user.id)
          do
            if stillAlted
            then Bus.pub(lila.core.security.GarbageCollect(user.id))

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
          if game.exists(_.playedPlies > 25) then funit
          else
            LilaFuture.delay(if game.isDefined then 12.seconds else 40.seconds):
              waitForCollection(userId, max)
