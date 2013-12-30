package lila.chat

import java.util.regex.Matcher.quoteReplacement

import play.api.i18n.Lang
import play.api.libs.json._

import lila.user.{ User, UserRepo }

private[chat] final class Api(
    namer: Namer,
    chanVoter: ChanVoter,
    flood: lila.security.Flood,
    relationApi: lila.relation.RelationApi,
    prefApi: lila.pref.PrefApi,
    netDomain: String) {

  def join(user: User, chat: ChatHead, chan: Chan): ChatHead = {
    chanVoter(user.id, chan.key)
    truncate(user, chat join chan, chan.key)
  }

  def join(member: ChatMember, chan: Chan): Fu[ChatHead] =
    getUser(member.userId) map { user ⇒ join(user, member.head, chan) }

  def show(user: User, chat: ChatHead, chan: Chan): ChatHead =
    truncate(user, chat.setChan(chan, true), chan.key)

  def truncate(user: User, chat: ChatHead, not: String): ChatHead =
    if (chat.chans.size <= Chat.maxChans) chat else {
      val nots = Set(LangChan(user).key, not)
      def filter(keys: Seq[String]) = keys filterNot nots.contains
      filter(chat.inactiveChanKeys).headOption match {
        case Some(key) ⇒ truncate(user, chat.unsetChanKey(key), not)
        case _ ⇒ chanVoter.lessVoted(user.id, filter(chat.chanKeys)) match {
          case Some(key) ⇒ truncate(user, chat.unsetChanKey(key), not)
          case _ ⇒ filter(chat.chanKeys).headOption match {
            case Some(key) ⇒ truncate(user, chat unsetChanKey key, not)
            case _         ⇒ chat
          }
        }
      }
    }

  def get(user: User): Fu[ChatHead] = prefApi getPref user map { pref ⇒
    show(user, ChatHead(pref.chat), LangChan(user))
  }

  def get(userId: String): Fu[ChatHead] = getUser(userId) flatMap get

  def populate(head: ChatHead, user: User): Fu[Chat] =
    namer.chans(head.chans, user) zip {
      relationApi blocking user.id flatMap {
        LineRepo.find(head.activeChanKeys, user.troll, _, 20) flatMap {
          _.map(namer.line).sequenceFu
        }
      }
    } map {
      case (namedChans, namedLines) ⇒ Chat(head, namedChans, namedLines.reverse)
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
        _ ?? (LineRepo insert line inject line.some)
      }
      case Some(line) ⇒ {
        logger.info(s"Flood: $userId @ $chanName : $text")
        fuccess(none)
      }
    }

  def systemWrite(chan: Chan, text: String): Fu[Line] = {
    val line = Line.system(chan, text)
    LineRepo insert line inject line
  }

  private[chat] def getUser(userId: String) =
    (UserRepo byId userId) flatten s"No such user: $userId"

  private val logger = play.api.Logger("chat")

  private object Writer {
    val delocalize = new lila.common.String.Delocalizer(netDomain)
    val domainRegex = netDomain.replace(".", """\.""")
    val urlRegex = (domainRegex + """/([\w-]{8})[\w-]{4}""").r
    def noPrivateUrl(str: String): String =
      urlRegex.replaceAllIn(str, m ⇒ quoteReplacement(netDomain + "/" + (m group 1)))
  }
}
