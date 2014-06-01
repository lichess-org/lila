package lila.chat

import chess.Color
import reactivemongo.bson.BSONDocument

import lila.db.Types.Coll
import lila.user.{ User, UserRepo }

private[chat] final class ChatApi(
    coll: Coll,
    flood: lila.security.Flood,
    maxLinesPerChat: Int,
    netDomain: String) {

  import Chat.userChatBSONHandler

  object userChat {

    def findOption(chatId: ChatId): Fu[Option[UserChat]] =
      coll.find(BSONDocument("_id" -> chatId)).one[UserChat]

    def find(chatId: ChatId): Fu[UserChat] =
      findOption(chatId) map (_ | Chat.makeUser(chatId))

    def write(chatId: ChatId, userId: String, text: String): Fu[Option[UserLine]] =
      makeLine(userId, text) flatMap {
        _ ?? { line => pushLine(chatId, line) inject line.some }
      }

    def system(chatId: ChatId, text: String) = {
      val line = UserLine(systemUserId, Writer delocalize text, false)
      pushLine(chatId, line) inject line.some
    }

    private[ChatApi] def makeLine(userId: String, t1: String): Fu[Option[UserLine]] = UserRepo byId userId map {
      _ flatMap { user =>
        Writer cut t1 ifFalse user.disabled flatMap { t2 =>
          flood.allowMessage(user.id, t2) option
            UserLine(user.username, Writer preprocessUserInput t2, user.troll)
        }
      }
    }
  }

  object playerChat {

    def findOption(chatId: ChatId): Fu[Option[MixedChat]] =
      coll.find(BSONDocument("_id" -> chatId)).one[MixedChat]

    def find(chatId: ChatId): Fu[MixedChat] =
      findOption(chatId) map (_ | Chat.makeMixed(chatId))

    def findNonEmpty(chatId: ChatId): Fu[Option[MixedChat]] =
      findOption(chatId) map (_ filter (_.nonEmpty))

    def write(chatId: ChatId, color: Color, text: String): Fu[Option[Line]] =
      makeLine(chatId, color, text) ?? { line =>
        pushLine(chatId, line) inject line.some
      }

    private def makeLine(chatId: ChatId, color: Color, t1: String): Option[Line] =
      Writer cut t1 flatMap { t2 =>
        flood.allowMessage(s"$chatId/${color.letter}", t2) option
          PlayerLine(color, Writer preprocessUserInput t2)
      }
  }

  private def pushLine(chatId: ChatId, line: Line) = coll.update(
    BSONDocument("_id" -> chatId),
    BSONDocument("$push" -> BSONDocument(
      Chat.BSONFields.lines -> BSONDocument(
        "$each" -> List(line),
        "$slice" -> -maxLinesPerChat)
    )),
    upsert = true)

  private object Writer {

    import java.util.regex.Matcher.quoteReplacement
    import org.apache.commons.lang3.StringEscapeUtils.escapeHtml4

    def preprocessUserInput(in: String) = delocalize(noPrivateUrl(escapeHtml4(in)))

    def cut(text: String) = Some(text.trim take 140) filter (_.nonEmpty)
    val delocalize = new lila.common.String.Delocalizer(netDomain)
    val domainRegex = netDomain.replace(".", """\.""")
    val gameUrlRegex = (domainRegex + """/([\w-]{8})[\w-]{4}""").r
    def noPrivateUrl(str: String): String =
      gameUrlRegex.replaceAllIn(str, m => quoteReplacement(netDomain + "/" + (m group 1)))
  }
}
