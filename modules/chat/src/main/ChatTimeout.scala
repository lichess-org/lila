package lila.chat

import lila.db.dsl._
import lila.user.User

import org.joda.time.DateTime
import reactivemongo.bson._
import scala.concurrent.duration._

final class ChatTimeout(
    coll: Coll,
    duration: FiniteDuration
) {

  import ChatTimeout._

  def add(chat: UserChat, mod: User, user: User, reason: Reason): Funit =
    isActive(chat.id, user.id) flatMap {
      case true => funit
      case false => coll.insert($doc(
        "_id" -> makeId,
        "chat" -> chat.id,
        "mod" -> mod.id,
        "user" -> user.id,
        "reason" -> reason,
        "createdAt" -> DateTime.now,
        "expiresAt" -> DateTime.now.plusSeconds(duration.toSeconds.toInt)
      )).void
    }

  def isActive(chatId: Chat.Id, userId: User.ID): Fu[Boolean] =
    coll.exists($doc(
      "chat" -> chatId,
      "user" -> userId,
      "expiresAt" $exists true
    ))

  def activeUserIds(chat: UserChat): Fu[List[User.ID]] =
    coll.primitive[User.ID]($doc(
      "chat" -> chat.id,
      "expiresAt" $exists true
    ), "user")

  def history(user: User, nb: Int): Fu[List[UserEntry]] =
    coll.find($doc("user" -> user.id)).sort($sort desc "createdAt").list[UserEntry](nb)

  def checkExpired: Fu[List[Reinstate]] = coll.list[Reinstate]($doc(
    "expiresAt" $lt DateTime.now
  )) flatMap {
    case Nil => fuccess(Nil)
    case objs =>
      coll.unsetField($inIds(objs.map(_._id)), "expiresAt", multi = true) inject objs
  }

  private val idSize = 8

  private def makeId = scala.util.Random.alphanumeric take idSize mkString
}

object ChatTimeout {

  sealed abstract class Reason(val key: String, val name: String)

  object Reason {
    case object PublicShaming extends Reason("shaming", "public shaming; please use lichess.org/report")
    case object Insult extends Reason("insult", "disrespecting other players")
    case object Spam extends Reason("spam", "spamming the chat")
    case object Other extends Reason("other", "inappropriate behavior")
    val all = List(PublicShaming, Insult, Spam, Other)
    def apply(key: String) = all.find(_.key == key)
  }
  implicit val ReasonBSONHandler: BSONHandler[BSONString, Reason] = new BSONHandler[BSONString, Reason] {
    def read(b: BSONString) = Reason(b.value) err s"Invalid reason ${b.value}"
    def write(x: Reason) = BSONString(x.key)
  }

  case class Reinstate(_id: String, chat: String, user: String)
  implicit val ReinstateBSONReader: BSONDocumentReader[Reinstate] = Macros.reader[Reinstate]

  case class UserEntry(mod: String, reason: Reason, createdAt: DateTime)
  implicit val UserEntryBSONReader: BSONDocumentReader[UserEntry] = Macros.reader[UserEntry]
}
