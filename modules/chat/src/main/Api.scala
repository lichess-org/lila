package lila.chat

import chess.Color
import reactivemongo.bson.BSONDocument

import lila.db.Types.Coll
import lila.user.{ User, UserRepo }

private[chat] final class Api(
    coll: Coll,
    flood: lila.security.Flood,
    maxLinesPerChat: Int,
    netDomain: String) {

  import Chat.userChatBSONHandler

  object userChat {

    def find(chatId: ChatId): Fu[Option[UserChat]] =
      coll.find(BSONDocument("_id" -> chatId)).one[UserChat]

    def write(chatId: ChatId, userId: String, text: String): Fu[Option[UserLine]] =
      makeLine(userId, text) flatMap {
        _ ?? { line ⇒ pushLine(chatId, line) inject line.some }
      }

    private[Api] def makeLine(userId: String, t1: String): Fu[Option[UserLine]] = UserRepo byId userId map {
      _ flatMap { user ⇒
        Writer cut t1 ifFalse user.disabled flatMap { t2 ⇒
          flood.allowMessage(user.id, t2) option
            UserLine(user.username, Writer preprocessUserInput t2, user.troll)
        }
      }
    }
  }

  object mixedChat {

    def find(chatId: ChatId): Fu[Option[MixedChat]] =
      coll.find(BSONDocument("_id" -> chatId)).one[MixedChat]

    def write(chatId: ChatId, user: Either[String, Color], text: String): Fu[Option[Line]] =
      makeLine(chatId, user, text) flatMap {
        _ ?? { line ⇒ pushLine(chatId, line) inject line.some }
      }

    private def makeLine(chatId: ChatId, user: Either[String, Color], t1: String): Fu[Option[Line]] = user match {
      case Left(userId) ⇒ userChat.makeLine(userId, t1)
      case Right(color) ⇒ fuccess {
        Writer cut t1 flatMap { t2 ⇒
          flood.allowMessage(s"$chatId/${color.letter}", t2) option
            PlayerLine(color, Writer preprocessUserInput t2)
        }
      }
    }
  }

  private def pushLine(chatId: ChatId, line: Line) = coll.update(
    BSONDocument("_id" -> chatId),
    BSONDocument("$push" -> BSONDocument(
      "messages" -> BSONDocument(
        "$each" -> line,
        "$slice" -> maxLinesPerChat)
    )),
    upsert = true)

  private object Writer {

    import java.util.regex.Matcher.quoteReplacement
    import org.apache.commons.lang3.StringEscapeUtils.escapeXml

    def preprocessUserInput(in: String) = addLinks(delocalize(noPrivateUrl(escapeXml(in))))

    def cut(text: String) = Some(text.trim take 200) filter (_.nonEmpty)
    def addLinks(text: String) = urlRegex.replaceAllIn(text, m ⇒ {
      val url = delocalize(quoteReplacement(m group 1))
      "<a target='_blank' href='%s'>%s</a>".format(prependHttp(url), url)
    })
    def prependHttp(url: String): String = url startsWith "http" fold (url, "http://" + url)
    val delocalize = new lila.common.String.Delocalizer(netDomain)
    val domainRegex = netDomain.replace(".", """\.""")
    val urlRegex = """(?i)\b((?:https?://|www\d{0,3}[.]|[a-z0-9.\-]+[.][a-z]{2,4}/)(?:[^\s()<>]+|\(([^\s()<>]+|(\([^\s()<>]+\)))*\))+(?:\(([^\s()<>]+|(\([^\s()<>]+\)))*\)|[^\s`!()\[\]{};:'".,<>?«»“”‘’]))""".r
    val gameUrlRegex = (domainRegex + """/([\w-]{8})[\w-]{4}""").r
    def noPrivateUrl(str: String): String =
      gameUrlRegex.replaceAllIn(str, m ⇒ quoteReplacement(netDomain + "/" + (m group 1)))
  }
}
