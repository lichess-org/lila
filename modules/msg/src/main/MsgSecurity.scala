package lila.msg

import org.joda.time.DateTime
import scala.concurrent.duration._

import lila.common.Bus
import lila.db.dsl._
import lila.hub.actorApi.report.AutoFlag
import lila.hub.actorApi.clas.IsTeacherOf
import lila.memo.RateLimit
import lila.shutup.Analyser
import lila.user.User

final private class MsgSecurity(
    colls: MsgColls,
    prefApi: lila.pref.PrefApi,
    userRepo: lila.user.UserRepo,
    relationApi: lila.relation.RelationApi,
    spam: lila.security.Spam,
    chatPanic: lila.chat.ChatPanic
)(implicit ec: scala.concurrent.ExecutionContext, system: akka.actor.ActorSystem) {

  import BsonHandlers._
  import MsgSecurity._

  private val CreateLimitPerUser = new RateLimit[User.ID](
    credits = 20 * 5,
    duration = 24 hour,
    name = "PM creates per user",
    key = "msg_create.user"
  )

  private val ReplyLimitPerUser = new RateLimit[User.ID](
    credits = 20 * 5,
    duration = 1 minute,
    name = "PM replies per user",
    key = "msg_reply.user"
  )

  object can {

    def post(contacts: User.Contacts, text: String, isNew: Boolean, unlimited: Boolean = false): Fu[Verdict] =
      may.post(contacts, isNew) flatMap {
        case false => fuccess(Block)
        case _ =>
          isLimited(contacts.orig, isNew, unlimited) orElse
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
          logger.warn(s"PM spam from ${contacts.orig.id}: ${text}")
        case _ =>
      }

    private def isLimited(user: User.Contact, isNew: Boolean, unlimited: Boolean): Fu[Option[Verdict]] =
      if (unlimited) fuccess(none)
      else {
        val limiter = if (isNew) CreateLimitPerUser else ReplyLimitPerUser
        val cost    = if (user.isVerified) 1 else 5
        !limiter(user.id, cost = cost)(true) ?? fuccess(Limit.some)
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
      userRepo.contacts(orig, dest) orFail "Missing convo contact user" flatMap {
        post(_, isNew)
      }

    def post(contacts: User.Contacts, isNew: Boolean): Fu[Boolean] =
      fuccess(contacts.dest.id != User.lichessId) >>&
        !relationApi.fetchBlocks(contacts.dest.id, contacts.orig.id) >>&
        (create(contacts) >>| reply(contacts)) >>&
        chatPanic.allowed(contacts.orig.id, userRepo.byId) >>&
        kidCheck(contacts, isNew)

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
      if (contacts.orig.isKid && isNew) fuFalse
      else if (!contacts.dest.isKid) fuTrue
      else Bus.ask[Boolean]("clas") { IsTeacherOf(contacts.orig.id, contacts.dest.id, _) }
  }
}

private object MsgSecurity {

  sealed trait Verdict
  sealed trait Reject                           extends Verdict
  sealed abstract class Send(val mute: Boolean) extends Verdict
  sealed abstract class Mute                    extends Send(true)

  case object Ok    extends Send(false)
  case object Troll extends Mute
  case object Spam  extends Mute
  case object Dirt  extends Mute
  case object Block extends Reject
  case object Limit extends Reject
}
