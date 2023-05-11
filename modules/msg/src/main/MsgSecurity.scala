package lila.msg

import lila.common.Bus
import lila.db.dsl.{ *, given }
import lila.hub.actorApi.clas.{ AreKidsInSameClass, IsTeacherOf }
import lila.hub.actorApi.report.AutoFlag
import lila.hub.actorApi.team.IsLeaderOf
import lila.memo.RateLimit
import lila.security.Granter
import lila.shutup.Analyser
import lila.user.User

final private class MsgSecurity(
    colls: MsgColls,
    prefApi: lila.pref.PrefApi,
    userRepo: lila.user.UserRepo,
    getBotUserIds: lila.user.GetBotIds,
    relationApi: lila.relation.RelationApi,
    spam: lila.security.Spam,
    chatPanic: lila.chat.ChatPanic
)(using
    ec: Executor,
    scheduler: Scheduler
):

  import MsgSecurity.*

  private object limitCost:
    val normal   = 25
    val verified = 5
    val hog      = 1
    def apply(u: User.Contact): Int =
      if (u.isApiHog) hog
      else if (u.isVerified) verified
      else if (u isDaysOld 30) normal
      else if (u isDaysOld 7) normal * 2
      else if (u isDaysOld 3) normal * 3
      else if (u isHoursOld 12) normal * 4
      else normal * 5

  private val CreateLimitPerUser = RateLimit[UserId](
    credits = 20 * limitCost.normal,
    duration = 24 hour,
    key = "msg_create.user"
  )

  private val ReplyLimitPerUser = RateLimit[UserId](
    credits = 20 * limitCost.normal,
    duration = 1 minute,
    key = "msg_reply.user"
  )

  private val dirtSpamDedup = lila.memo.OnceEvery.hashCode[String](1 minute)

  object can:

    def post(
        contacts: User.Contacts,
        rawText: String,
        isNew: Boolean,
        unlimited: Boolean = false
    ): Fu[Verdict] =
      val text = rawText.trim
      if (text.isEmpty) fuccess(Invalid)
      else if (contacts.orig.isLichess && !contacts.dest.isLichess) fuccess(Ok)
      else
        may.post(contacts, isNew) flatMap {
          case false => fuccess(Block)
          case _ =>
            isLimited(contacts, isNew, unlimited, text) orElse
              isFakeTeamMessage(rawText, unlimited) orElse
              isSpam(text) orElse
              isTroll(contacts) orElse
              isDirt(contacts.orig, text, isNew) getOrElse
              fuccess(Ok)
        } flatMap {
          case mute: Mute =>
            relationApi.fetchFollows(contacts.dest.id, contacts.orig.id) dmap { isFriend =>
              if (isFriend) Ok else mute
            }
          case verdict => fuccess(verdict)
        } addEffect {
          case Dirt =>
            if (dirtSpamDedup(text))
              Bus.publish(
                AutoFlag(
                  contacts.orig.id,
                  s"msg/${contacts.orig.id}/${contacts.dest.id}",
                  text,
                  Analyser.isCritical(text)
                ),
                "autoFlag"
              )

          case Spam =>
            if (dirtSpamDedup(text) && !contacts.orig.isTroll)
              logger.warn(s"PM spam from ${contacts.orig.id} to ${contacts.dest.id}: $text")

          case _ =>
        }

    private def isLimited(
        contacts: User.Contacts,
        isNew: Boolean,
        unlimited: Boolean,
        text: String
    ): Fu[Option[Verdict]] =
      def limitWith(limiter: RateLimit[UserId]) =
        val cost = limitCost(contacts.orig) * {
          if !contacts.orig.isVerified && Analyser.containsLink(text) then 2 else 1
        }
        limiter(contacts.orig.id, Limit.some, cost)(none)
      if (unlimited) fuccess(none)
      else if (isNew) {
        isLeaderOf(contacts) >>| isTeacherOf(contacts)
      } map {
        if _ then none
        else limitWith(CreateLimitPerUser)
      }
      else fuccess(limitWith(ReplyLimitPerUser))

    private def isFakeTeamMessage(text: String, unlimited: Boolean): Fu[Option[Verdict]] =
      (!unlimited && text.contains("You received this because you are subscribed to messages of the team")) ??
        fuccess(FakeTeamMessage.some)

    private def isSpam(text: String): Fu[Option[Verdict]] =
      spam.detect(text) ?? fuccess(Spam.some)

    private def isTroll(contacts: User.Contacts): Fu[Option[Verdict]] =
      contacts.orig.isTroll ?? fuccess(Troll.some)

    private def isDirt(user: User.Contact, text: String, isNew: Boolean): Fu[Option[Verdict]] =
      (isNew && Analyser(text).dirty) ??
        !userRepo.isCreatedSince(user.id, nowInstant.minusDays(30)) dmap { _ option Dirt }

  object may:

    def post(orig: UserId, dest: UserId, isNew: Boolean): Fu[Boolean] =
      userRepo.contacts(orig, dest) flatMapz { post(_, isNew) }

    def post(contacts: User.Contacts, isNew: Boolean): Fu[Boolean] =
      fuccess(!contacts.dest.isLichess) >>& {
        fuccess(Granter.byRoles(_.PublicMod)(~contacts.orig.roles)) >>| {
          !relationApi.fetchBlocks(contacts.dest.id, contacts.orig.id) >>&
            (create(contacts) >>| reply(contacts)) >>&
            chatPanic.allowed(contacts.orig.id, userRepo.byId) >>&
            kidCheck(contacts, isNew) >>&
            getBotUserIds().map { botIds => !contacts.userIds.exists(botIds.contains) }
        }
      }

    private def create(contacts: User.Contacts): Fu[Boolean] =
      prefApi.getPref(contacts.dest.id, _.message) flatMap {
        case lila.pref.Pref.Message.NEVER  => fuccess(false)
        case lila.pref.Pref.Message.FRIEND => relationApi.fetchFollows(contacts.dest.id, contacts.orig.id)
        case lila.pref.Pref.Message.ALWAYS => fuccess(true)
      }

    // Even if the dest prefs disallow it,
    // you can still reply if they recently messaged you,
    // unless they deleted the thread.
    private def reply(contacts: User.Contacts): Fu[Boolean] =
      colls.thread.exists(
        $id(MsgThread.id(contacts.orig.id, contacts.dest.id)) ++
          $doc("del" $ne contacts.dest.id)
      )

    private def kidCheck(contacts: User.Contacts, isNew: Boolean): Fu[Boolean] =
      import contacts.*
      if (!isNew || !hasKid) fuTrue
      else
        (orig.isKid, dest.isKid) match
          case (true, true)  => Bus.ask[Boolean]("clas") { AreKidsInSameClass(orig.id, dest.id, _) }
          case (false, true) => isTeacherOf(orig.id, dest.id)
          case (true, false) => isTeacherOf(dest.id, orig.id)
          case _             => fuFalse

  private def isTeacherOf(contacts: User.Contacts): Fu[Boolean] =
    isTeacherOf(contacts.orig.id, contacts.dest.id)

  private def isTeacherOf(teacher: UserId, student: UserId): Fu[Boolean] =
    Bus.ask[Boolean]("clas") { IsTeacherOf(teacher, student, _) }

  private def isLeaderOf(contacts: User.Contacts) =
    Bus.ask[Boolean]("teamIsLeaderOf") { IsLeaderOf(contacts.orig.id, contacts.dest.id, _) }

private object MsgSecurity:

  sealed trait Verdict
  sealed trait Reject                           extends Verdict
  sealed abstract class Send(val mute: Boolean) extends Verdict
  sealed abstract class Mute                    extends Send(true)

  case object Ok              extends Send(false)
  case object Troll           extends Mute
  case object Spam            extends Mute
  case object Dirt            extends Mute
  case object FakeTeamMessage extends Reject
  case object Block           extends Reject
  case object Limit           extends Reject
  case object Invalid         extends Reject
  case object BotUser         extends Reject
