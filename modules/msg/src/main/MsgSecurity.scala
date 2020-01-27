package lila.msg

import org.joda.time.DateTime
import scala.concurrent.duration._

import lila.common.Bus
import lila.db.dsl._
import lila.hub.actorApi.report.AutoFlag
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
)(implicit ec: scala.concurrent.ExecutionContext) {

  import BsonHandlers._
  import MsgSecurity._

  private val CreateLimitPerUser = new RateLimit[User.ID](
    credits = 20,
    duration = 24 hour,
    name = "PM creates per user",
    key = "msg_create.user"
  )

  private val ReplyLimitPerUser = new RateLimit[User.ID](
    credits = 20,
    duration = 1 minute,
    name = "PM replies per user",
    key = "msg_reply.user"
  )

  object can {

    def post(dest: User.ID, msg: Msg, isNew: Boolean): Fu[Verdict] =
      may.post(msg.user, dest) flatMap {
        case false => fuccess(Block)
        case _ =>
          isLimited(msg, isNew) orElse
            isSpam(msg) orElse
            isTroll(msg.user, dest) orElse
            isDirt(msg, isNew) getOrElse
            fuccess(Ok)
      } flatMap {
        case mute: Mute =>
          relationApi.fetchFollows(dest, msg.user) dmap { isFriend =>
            if (isFriend) Ok else mute
          }
        case verdict => fuccess(verdict)
      } addEffect {
        case Dirt =>
          Bus.publish(AutoFlag(msg.user, s"msg/${msg.user}/$dest", msg.text), "autoFlag")
        case Spam =>
          logger.warn(s"PM spam from ${msg.user}: ${msg.text}")
        case _ =>
      }

    private def isLimited(msg: Msg, isNew: Boolean): Fu[Option[Verdict]] = {
      val limiter = if (isNew) CreateLimitPerUser else ReplyLimitPerUser
      !limiter(msg.user)(true) ?? fuccess(Limit.some)
    }

    private def isSpam(msg: Msg): Fu[Option[Verdict]] =
      spam.detect(msg.text) ?? fuccess(Spam.some)

    private def isTroll(orig: User.ID, dest: User.ID): Fu[Option[Verdict]] =
      userRepo.isTroll(orig) >>& !userRepo.isTroll(dest) dmap { _ option Troll }

    private def isDirt(msg: Msg, isNew: Boolean): Fu[Option[Verdict]] =
      (isNew && Analyser(msg.text).dirty) ??
        !userRepo.isCreatedSince(msg.user, DateTime.now.minusDays(30)) dmap { _ option Dirt }
  }

  object may {

    def post(orig: User.ID, dest: User.ID): Fu[Boolean] = (dest != User.lichessId) ?? {
      !relationApi.fetchBlocks(dest, orig) >>& {
        create(orig, dest) >>| reply(orig, dest)
      } >>& chatPanic.allowed(orig, userRepo.byId)
    }

    private def create(orig: User.ID, dest: User.ID): Fu[Boolean] =
      prefApi.getPref(dest, _.message) flatMap {
        case lila.pref.Pref.Message.NEVER  => fuccess(false)
        case lila.pref.Pref.Message.FRIEND => relationApi.fetchFollows(dest, orig)
        case lila.pref.Pref.Message.ALWAYS => fuccess(true)
      }

    // Even if the dest prefs disallow it,
    // you can still reply if they recently messaged you,
    // unless they deleted the thread.
    private def reply(orig: User.ID, dest: User.ID): Fu[Boolean] =
      colls.thread.exists(
        $id(MsgThread.id(orig, dest)) ++ $or(
          "del" $ne dest,
          $doc(
            "lastMsg.user" -> dest,
            "lastMsg.date" $gt DateTime.now.minusDays(3)
          )
        )
      )
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
