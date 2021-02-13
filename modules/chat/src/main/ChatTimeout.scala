package lila.chat

import lila.db.dsl._
import lila.user.User

import org.joda.time.DateTime
import reactivemongo.api.bson._
import scala.concurrent.duration._

final class ChatTimeout(
    coll: Coll,
    duration: FiniteDuration
)(implicit ec: scala.concurrent.ExecutionContext) {

  import ChatTimeout._

  private val global = new lila.memo.ExpireSetMemo(duration)

  def add(chat: UserChat, mod: User, user: User, reason: Reason, scope: Scope): Funit =
    isActive(chat.id, user.id) flatMap {
      case true => funit
      case false =>
        if (scope == Scope.Global) global put user.id
        coll.insert
          .one(
            $doc(
              "_id"       -> makeId,
              "chat"      -> chat.id,
              "mod"       -> mod.id,
              "user"      -> user.id,
              "reason"    -> reason,
              "createdAt" -> DateTime.now,
              "expiresAt" -> DateTime.now.plusSeconds(duration.toSeconds.toInt)
            )
          )
          .void
    }

  def isActive(chatId: Chat.Id, userId: User.ID): Fu[Boolean] =
    fuccess(global.get(userId)) >>| coll.exists(
      $doc(
        "chat" -> chatId,
        "user" -> userId,
        "expiresAt" $exists true
      )
    )

  def history(user: User, nb: Int): Fu[List[UserEntry]] =
    coll.find($doc("user" -> user.id)).sort($sort desc "createdAt").cursor[UserEntry]().list(nb)

  def checkExpired: Fu[List[Reinstate]] =
    coll.list[Reinstate](
      $doc(
        "expiresAt" $lt DateTime.now
      )
    ) flatMap {
      case Nil => fuccess(Nil)
      case objs =>
        coll.unsetField($inIds(objs.map(_._id)), "expiresAt", multi = true) inject objs
    }

  private val idSize = 8

  private def makeId = lila.common.ThreadLocalRandom nextString idSize
}

object ChatTimeout {

  sealed abstract class Reason(val key: String, val name: String)

  object Reason {
    case object PublicShaming extends Reason("shaming", "public shaming; please use lichess.org/report")
    case object Insult        extends Reason("insult", "disrespecting other players")
    case object Spam          extends Reason("spam", "spamming the chat")
    case object Other         extends Reason("other", "inappropriate behavior")
    val all: List[Reason]  = List(PublicShaming, Insult, Spam, Other)
    def apply(key: String) = all.find(_.key == key)
  }
  implicit val ReasonBSONHandler: BSONHandler[Reason] = tryHandler[Reason](
    { case BSONString(value) => Reason(value) toTry s"Invalid reason $value" },
    x => BSONString(x.key)
  )

  case class Reinstate(_id: String, chat: String, user: String)
  implicit val ReinstateBSONReader: BSONDocumentReader[Reinstate] = Macros.reader[Reinstate]

  case class UserEntry(mod: String, reason: Reason, createdAt: DateTime)
  implicit val UserEntryBSONReader: BSONDocumentReader[UserEntry] = Macros.reader[UserEntry]

  sealed trait Scope
  object Scope {
    case object Local  extends Scope
    case object Global extends Scope
  }
}
