package lila.msg

import org.joda.time.DateTime
import scala.concurrent.duration._

import lila.common.Bus
import lila.db.dsl._
import lila.hub.actorApi.clas.IsTeacherOf
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
    relationApi: lila.relation.RelationApi,
    spam: lila.security.Spam,
    chatPanic: lila.chat.ChatPanic
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: akka.actor.ActorSystem
) {

  import BsonHandlers._
  import MsgSecurity._

  private object limitCost {
    val normal   = 25
    val verified = 5
    val hog      = 1
    def apply(u: User.Contact) =
      if (u.isApiHog) hog
      else if (u.isVerified) verified
      else if (u isDaysOld 3) normal
      else if (u isHoursOld 3) normal * 2
      else normal * 4
  }

  private val CreateLimitPerUser = new RateLimit[User.ID](
    credits = 20 * limitCost.normal,
    duration = 24 hour,
    key = "msg_create.user"
  )

  private val ReplyLimitPerUser = new RateLimit[User.ID](
    credits = 20 * limitCost.normal,
    duration = 1 minute,
    key = "msg_reply.user"
  )

  object can {

    def post(
        contacts: User.Contacts,
        rawText: String,
        isNew: Boolean,
        unlimited: Boolean = false
    ): Fu[Verdict] = {
      val text = rawText.trim
      if (text.isEmpty) fuccess(Invalid)
      else
        may.post(contacts, isNew) flatMap {
          case false => fuccess(Block)
          case _ =>
            isLimited(contacts, isNew, unlimited) orElse
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
            Bus.publish(
              AutoFlag(contacts.orig.id, s"msg/${contacts.orig.id}/${contacts.dest.id}", text),
              "autoFlag"
            )
          case Spam =>
            logger.warn(s"PM spam from ${contacts.orig.id}: $text")
          case _ =>
        }
    }

    private def isLimited(contacts: User.Contacts, isNew: Boolean, unlimited: Boolean): Fu[Option[Verdict]] =
      if (unlimited) fuccess(none)
      else if (isNew) {
        isLeaderOf(contacts) >>| isTeacherOf(contacts)
      } map {
        case true => none
        case _ =>
          CreateLimitPerUser[Option[Verdict]](contacts.orig.id, limitCost(contacts.orig))(none)(Limit.some)
      }
      else
        fuccess {
          ReplyLimitPerUser[Option[Verdict]](contacts.orig.id, limitCost(contacts.orig))(none)(Limit.some)
        }

    private def isSpam(text: String): Fu[Option[Verdict]] =
      spam.detect(text) ?? fuccess(Spam.some)

    private def isTroll(contacts: User.Contacts): Fu[Option[Verdict]] =
      (contacts.orig.isTroll && !contacts.dest.isTroll) ?? fuccess(Troll.some)

    private def isDirt(user: User.Contact, text: String, isNew: Boolean): Fu[Option[Verdict]] =
      (isNew && Analyser(text).dirty) ??
        !userRepo.isCreatedSince(user.id, DateTime.now.minusDays(30)) dmap { _ option Dirt }
  }

  object may {

    def post(orig: User.ID, dest: User.ID, isNew: Boolean): Fu[Boolean] =
      userRepo.contacts(orig, dest) flatMap {
        _ ?? { post(_, isNew) }
      }

    def post(contacts: User.Contacts, isNew: Boolean): Fu[Boolean] =
      fuccess(contacts.dest.id != User.lichessId) >>& {
        fuccess(Granter.byRoles(_.ModMessage)(~contacts.orig.roles)) >>| {
          !relationApi.fetchBlocks(contacts.dest.id, contacts.orig.id) >>&
            (create(contacts) >>| reply(contacts)) >>&
            chatPanic.allowed(contacts.orig.id, userRepo.byId) >>&
            kidCheck(contacts, isNew)
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
        $id(MsgThread.id(contacts.orig.id, contacts.dest.id)) ++ $or(
          "del" $ne contacts.dest.id,
          $doc(
            "lastMsg.user" -> contacts.dest.id,
            "lastMsg.date" $gt DateTime.now.minusDays(3)
          )
        )
      )

    private def kidCheck(contacts: User.Contacts, isNew: Boolean): Fu[Boolean] =
      if (contacts.orig.isKid && isNew) isTeacherOf(contacts)
      else if (!contacts.dest.isKid) fuTrue
      else isTeacherOf(contacts)
  }

  private def isTeacherOf(contacts: User.Contacts) =
    Bus.ask[Boolean]("clas") { IsTeacherOf(contacts.orig.id, contacts.dest.id, _) }

  private def isLeaderOf(contacts: User.Contacts) =
    Bus.ask[Boolean]("teamIsLeaderOf") { IsLeaderOf(contacts.orig.id, contacts.dest.id, _) }
}

private object MsgSecurity {

  sealed trait Verdict
  sealed trait Reject                           extends Verdict
  sealed abstract class Send(val mute: Boolean) extends Verdict
  sealed abstract class Mute                    extends Send(true)

  case object Ok      extends Send(false)
  case object Troll   extends Mute
  case object Spam    extends Mute
  case object Dirt    extends Mute
  case object Block   extends Reject
  case object Limit   extends Reject
  case object Invalid extends Reject
}
