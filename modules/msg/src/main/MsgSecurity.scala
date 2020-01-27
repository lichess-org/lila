package lila.msg

import org.joda.time.DateTime
import scala.concurrent.duration._

import lila.db.dsl._
import lila.memo.RateLimit
import lila.user.User

final private class MsgSecurity(
    colls: MsgColls,
    prefApi: lila.pref.PrefApi,
    userRepo: lila.user.UserRepo,
    relationApi: lila.relation.RelationApi
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

  def post(orig: User.ID, dest: User.ID, msg: Msg, isNew: Boolean): Fu[Verdict] =
    may.post(orig, dest) flatMap {
      case false => fuccess(Block)
      case _ =>
        val limiter = if (isNew) CreateLimitPerUser else ReplyLimitPerUser
        if (!limiter(orig)(true)) fuccess(Limit)
        else
          muteTroll(orig, dest) map { troll =>
            Ok(mute = troll)
          }
    }

  private def muteTroll(orig: User.ID, dest: User.ID): Fu[Boolean] =
    userRepo.isTroll(orig) >>&
      !userRepo.isTroll(dest) >>&
      !relationApi.fetchFollows(dest, orig)

  object may {

    def post(orig: User.ID, dest: User.ID): Fu[Boolean] =
      !relationApi.fetchBlocks(dest, orig) >>& {
        create(orig, dest) >>| reply(orig, dest)
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

  case class Ok(mute: Boolean) extends Verdict
  case object Block            extends Verdict
  case object Limit            extends Verdict
}
