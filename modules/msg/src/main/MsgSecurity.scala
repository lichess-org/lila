package lila.msg

import lila.common.Bus
import lila.core.misc.clas.ClasBus
import lila.core.report.SuspectId
import lila.core.shutup.TextAnalyser
import lila.core.team.IsLeaderOf
import lila.db.dsl.{ *, given }
import lila.memo.RateLimit

final private class MsgSecurity(
    colls: MsgColls,
    contactApi: ContactApi,
    prefApi: lila.core.pref.PrefApi,
    userApi: lila.core.user.UserApi,
    userCache: lila.core.user.CachedApi,
    relationApi: lila.core.relation.RelationApi,
    reportApi: lila.core.report.ReportApi,
    spam: lila.core.security.SpamApi,
    textAnalyser: TextAnalyser
)(using Executor, Scheduler, lila.core.config.RateLimit):

  import MsgSecurity.*

  private object limitCost:
    val normal = 25
    def apply(u: Contact, isNew: Boolean): Int =
      if u.isApiHog then 1
      else if u.isVerified then 5
      else if u.isBroadcastManager then 5
      else if isNew && !u.seenRecently then normal * 5
      else if u.isDaysOld(30) then normal
      else if u.isDaysOld(7) then normal * 2
      else if u.isDaysOld(3) then normal * 3
      else if u.isHoursOld(12) then normal * 4
      else normal * 5

  private val CreateLimitPerUser = RateLimit[UserId](
    credits = 20 * limitCost.normal,
    duration = 24.hour,
    key = "msg_create.user"
  )

  private val ReplyLimitPerUser = RateLimit[UserId](
    credits = 20 * limitCost.normal,
    duration = 1.minute,
    key = "msg_reply.user"
  )

  private val dirtSpamDedup = scalalib.cache.OnceEvery.hashCode[String](1.minute)

  object can:

    def post(
        contacts: Contacts,
        rawText: String,
        isNew: Boolean,
        unlimited: Boolean = false
    ): Fu[Verdict] =
      val text = rawText.trim
      if text.isEmpty then fuccess(Invalid)
      else if contacts.orig.isLichess && !contacts.dest.isLichess then fuccess(Ok)
      else
        may
          .post(contacts, isNew)
          .flatMap:
            case false => fuccess(Block)
            case _ =>
              isLimited(contacts, isNew, unlimited, text)
                .orElse(isFakeTeamMessage(rawText, unlimited))
                .orElse(isSpam(text))
                .orElse(isTroll(contacts))
                .orElse(isDirt(contacts.orig, text, isNew))
                .getOrElse(fuccess(Ok))
          .flatMap:
            case Troll =>
              (fuccess(contacts.any(_.isGranted(_.PublicMod))) >>|
                destFollowsOrig(contacts)).dmap:
                if _ then TrollFriend else Troll
            case mute: Mute =>
              destFollowsOrig(contacts).dmap:
                if _ then Ok else mute
            case verdict => fuccess(verdict)
          .addEffect:
            case Dirt =>
              if dirtSpamDedup(text) then
                val resource = s"msg/${contacts.orig.id}/${contacts.dest.id}"
                reportApi.autoCommFlag(
                  SuspectId(contacts.orig.id),
                  resource,
                  text,
                  textAnalyser.isCritical(text)
                )
            case Spam =>
              if dirtSpamDedup(text) && !contacts.orig.isTroll
              then logger.warn(s"PM spam from ${contacts.orig.id} to ${contacts.dest.id}: $text")
            case _ =>

    private def isLimited(
        contacts: Contacts,
        isNew: Boolean,
        unlimited: Boolean,
        text: String
    ): Fu[Option[Verdict]] =
      def limitWith(limiter: RateLimit[UserId]) =
        val cost = limitCost(contacts.orig, isNew) * {
          if !contacts.orig.isVerified && textAnalyser.containsLink(text) then 2 else 1
        }
        limiter(contacts.orig.id, Limit.some, cost)(none)
      if unlimited then fuccess(none)
      else if isNew then
        (isLeaderOf(contacts) >>| isTeacherOf(contacts)).map:
          if _ then none
          else limitWith(CreateLimitPerUser)
      else fuccess(limitWith(ReplyLimitPerUser))

    private def isFakeTeamMessage(text: String, unlimited: Boolean): Fu[Option[Verdict]] =
      (!unlimited && text.contains("You received this because you are subscribed to messages of the team"))
        .so(fuccess(FakeTeamMessage.some))

    private def isSpam(text: String): Fu[Option[Verdict]] =
      spam.detect(text).so(fuccess(Spam.some))

    private def isTroll(contacts: Contacts): Fu[Option[Verdict]] =
      contacts.orig.isTroll.so(fuccess(Troll.some))

    private def isDirt(user: Contact, text: String, isNew: Boolean): Fu[Option[Verdict]] =
      (isNew && textAnalyser(text).dirty)
        .so(userApi.isCreatedSince(user.id, nowInstant.minusDays(30)).not)
        .dmap(_.option(Dirt))

    private def destFollowsOrig(contacts: Contacts): Fu[Boolean] =
      relationApi.fetchFollows(contacts.dest.id, contacts.orig.id)

  object may:

    def post(orig: UserId, dest: UserId, isNew: Boolean): Fu[Boolean] =
      contactApi.contacts(orig, dest).flatMapz { post(_, isNew) }

    def post(contacts: Contacts, isNew: Boolean): Fu[Boolean] =
      (
        !contacts.dest.isLichess &&
          (!contacts.any(_.marks.exists(_.isolate)) ||
            contacts.any(c => c.isGranted(_.Shadowban) && c.isGranted(_.PublicMod)))
      ).so:
        fuccess(contacts.orig.isGranted(_.PublicMod)) >>| {
          relationApi.fetchBlocks(contacts.dest.id, contacts.orig.id).not >>&
            (create(contacts) >>| reply(contacts)) >>&
            kidCheck(contacts, isNew) >>&
            userCache.getBotIds.map { botIds => !contacts.userIds.exists(botIds.contains) }
        }

    private def create(contacts: Contacts): Fu[Boolean] =
      prefApi
        .getMessage(contacts.dest.id)
        .flatMap:
          case lila.core.pref.Message.NEVER => fuccess(false)
          case lila.core.pref.Message.FRIEND => relationApi.fetchFollows(contacts.dest.id, contacts.orig.id)
          case lila.core.pref.Message.ALWAYS => fuccess(true)

    // Even if the dest prefs disallow it,
    // you can still reply if they recently messaged you,
    // unless they deleted the thread.
    private def reply(contacts: Contacts): Fu[Boolean] =
      colls.thread.exists(
        $id(MsgThread.id(contacts.orig.id, contacts.dest.id)) ++
          $doc("del".$ne(contacts.dest.id))
      )

    private def kidCheck(contacts: Contacts, isNew: Boolean): Fu[Boolean] =
      import contacts.*
      if !isNew || !hasKid then fuTrue
      else
        (orig.isKid, dest.isKid) match
          case (true, true) =>
            Bus.ask[Boolean, ClasBus] { ClasBus.AreKidsInSameClass(orig.id, dest.id, _) }
          case (false, true) => isTeacherOf(orig.id, dest.id)
          case (true, false) => isTeacherOf(dest.id, orig.id)
          case _ => fuFalse

  private def isTeacherOf(contacts: Contacts): Fu[Boolean] =
    isTeacherOf(contacts.orig.id, contacts.dest.id)

  private def isTeacherOf(teacher: UserId, student: UserId): Fu[Boolean] =
    Bus.ask[Boolean, ClasBus] { ClasBus.IsTeacherOf(teacher, student, _) }

  private def isLeaderOf(contacts: Contacts) =
    Bus.ask[Boolean, IsLeaderOf](IsLeaderOf(contacts.orig.id, contacts.dest.id, _))

private object MsgSecurity:

  sealed trait Verdict
  sealed trait Reject extends Verdict
  sealed abstract class Send(val mute: Boolean) extends Verdict
  sealed abstract class Mute extends Send(mute = true)

  case object Ok extends Send(mute = false)
  case object TrollFriend extends Send(mute = false)
  case object Troll extends Mute
  case object Spam extends Mute
  case object Dirt extends Mute
  case object FakeTeamMessage extends Reject
  case object Block extends Reject
  case object Limit extends Reject
  case object Invalid extends Reject
  case object BotUser extends Reject
