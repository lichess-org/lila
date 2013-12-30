package lila.chat

import java.util.regex.Matcher.quoteReplacement

import Line.{ BSONFields ⇒ L }
import org.apache.commons.lang3.StringEscapeUtils.escapeXml
import play.api.i18n.Lang
import play.api.libs.json._

import lila.db.api._
import lila.db.Implicits._
import lila.user.{ User, UserRepo }
import tube.lineTube

private[chat] final class Api(
    namer: Namer,
    flood: lila.security.Flood,
    relationApi: lila.relation.RelationApi,
    prefApi: lila.pref.PrefApi,
    netDomain: String) {

  def get(user: User): Fu[ChatHead] = prefApi getPref user flatMap { p ⇒
    val langChan = LangChan(Lang(user.lang | "en"))
    p.chat.isDefault.fold({
      val p2 = p.updateChat(c ⇒ ChatHead(c) join langChan updatePref c)
      prefApi setPref p2 inject p2.chat
    }, fuccess(p.chat)) map { pref ⇒
      ChatHead(pref).setChan(langChan, true)
    }
  }

  def get(userId: String): Fu[ChatHead] =
    (UserRepo byId userId) flatten s"No such user: $userId" flatMap get

  def populate(head: ChatHead, user: User): Fu[Chat] =
    relationApi blocking user.id flatMap { blocks ⇒
      val selectTroll = user.troll.fold(Json.obj(), Json.obj(L.troll -> false))
      val selectBlock = Json.obj(L.username -> $nin(blocks))
      namer.chans(head.chans, user) zip
        $find($query(
          selectTroll ++
            selectBlock ++
            Json.obj(L.chan -> $in(head.activeChanKeys))
        ) sort $sort.desc(L.date), 20) map {
          case (namedChans, lines) ⇒ Chat(head, namedChans, lines.reverse)
        }
    }

  def makeLine(chanName: String, userId: String, t1: String): Fu[Option[Line]] =
    UserRepo byId userId map { userOption ⇒
      import Writer._
      for {
        user ← userOption
        chan ← Chan parse chanName
        t2 ← Some(t1.trim take 200) filter (_.nonEmpty)
        if !user.disabled
      } yield Line.make(chan, user, delocalize(noPrivateUrl(t2)))
    }

  def write(chanName: String, userId: String, text: String): Fu[Option[Line]] =
    makeLine(chanName, userId, text) flatMap {
      case None ⇒ {
        logger.info(s"$userId @ $chanName : $text")
        fuccess(none)
      }
      case Some(line) if flood.allowMessage(line.userId, line.text) ⇒ (line.chan match {
        case UserChan(u1, u2) ⇒ relationApi.areFriends(u1, u2)
        case _                ⇒ fuccess(true)
      }) flatMap {
        case true  ⇒ write(line) inject line.some
        case false ⇒ fuccess(none)
      }
      case Some(line) ⇒ {
        logger.info(s"Flood: $userId @ $chanName : $text")
        fuccess(none)
      }
    }

  def write(line: Line): Funit = $insert bson line

  def systemWrite(chan: Chan, text: String): Fu[Line] = {
    val line = Line.system(chan, text)
    $insert bson line inject line
  }

  private val logger = play.api.Logger("chat")

  private object Writer {
    val delocalize = new lila.common.String.Delocalizer(netDomain)
    val domainRegex = netDomain.replace(".", """\.""")
    val urlRegex = (domainRegex + """/([\w-]{8})[\w-]{4}""").r
    def noPrivateUrl(str: String): String =
      urlRegex.replaceAllIn(str, m ⇒ quoteReplacement(netDomain + "/" + (m group 1)))
  }
}
