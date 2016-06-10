package lila.chat

import lila.db.dsl._
import lila.user.{ User, UserRepo }

import org.joda.time.DateTime
import reactivemongo.bson._

final class ChatTimeout(
    chatColl: Coll,
    timeoutColl: Coll) {

  import ChatTimeout._

  private val minutes = 10

  def add(chatId: ChatId, modId: String, username: String, reason: Reason): Funit =
    chatColl.byId[UserChat](chatId) zip UserRepo.named(modId) zip UserRepo.named(username) flatMap {
      case ((Some(chat), Some(mod)), Some(user)) if isMod(mod) => add(chat, mod, user, reason.pp)
      case _ => fuccess(none)
    }

  private def add(chat: UserChat, mod: User, user: User, reason: Reason): Funit =
    isActive(chat, user).thenPp flatMap {
      case true => funit
      case false => timeoutColl.insert($doc(
        "_id" -> makeId,
        "chat" -> chat.id,
        "mod" -> mod.id,
        "user" -> user.id,
        "reason" -> reason,
        "createdAt" -> DateTime.now,
        "expiresAt" -> DateTime.now.plusMinutes(minutes))).void
    }

  def isActive(chat: UserChat, user: User): Fu[Boolean] =
    timeoutColl.exists($doc(
      "chat" -> chat.id,
      "user" -> user.id,
      "expiresAt" $exists true))

  def activeUserIds(chat: UserChat): Fu[List[String]] =
    timeoutColl.primitive[String]($doc(
      "chat" -> chat.id,
      "expiresAt" $exists true
    ), "user")

  def checkExpired: Funit = timeoutColl.primitive[String]($doc(
    "expiresAt" $lt DateTime.now
  ), "_id") flatMap {
    case Nil => funit
    case ids => timeoutColl.unsetField($inIds(ids), "expiresAt", multi = true).void
  }

  private val idSize = 8

  private def makeId = scala.util.Random.alphanumeric take idSize mkString

  private def isMod(user: User) = lila.security.Granter(_.MarkTroll.pp)(user.pp).pp
}

object ChatTimeout {

  sealed abstract class Reason(val key: String, val name: String)

  object Reason {
    case object PublicShaming extends Reason("shaming", "Public shaming; please use lichess.org/report")
    case object Insult extends Reason("insult", "Disrespecting other players")
    case object Spam extends Reason("spam", "Spamming the chat")
    case object Other extends Reason("other", "Inappropriate behavior")
    val all = List(PublicShaming, Insult, Spam, Other)
    def apply(key: String) = all.find(_.key == key)
  }
  implicit val ReasonBSONHandler: BSONHandler[BSONString, Reason] = new BSONHandler[BSONString, Reason] {
    def read(b: BSONString) = Reason(b.value) err s"Invalid reason ${b.value}"
    def write(x: Reason) = BSONString(x.key)
  }
}
