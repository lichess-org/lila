package lila.chat

import play.api.i18n.Lang
import play.api.libs.json._

import lila.user.{ User, UserRepo }

private[chat] final class Api(
    namer: Namer,
    chanVoter: ChanVoter,
    flood: lila.security.Flood,
    relationApi: lila.relation.RelationApi,
    prefApi: lila.pref.PrefApi,
    getTeamIds: String ⇒ Fu[List[String]],
    netDomain: String) {

  private val NB_LINES = 40

  def join(user: User, chat: ChatHead, chan: Chan): ChatHead = {
    chanVoter(user.id, chan.key)
    truncate(user, chat join chan, chan.key)
  }

  def join(member: ChatMember, chan: Chan): Fu[ChatHead] =
    getUser(member.userId) map { user ⇒ join(user, member.head, chan) }

  def show(user: User, chat: ChatHead, chan: Chan): ChatHead =
    truncate(user, chat.setChan(chan, true), chan.key)

  def active(user: User, chat: ChatHead, chan: Chan): ChatHead = {
    chanVoter(user.id, chan.key)
    truncate(user, chat.ensureActiveChan(chan), chan.key)
  }

  def active(member: ChatMember, chan: Chan): Fu[ChatHead] =
    getUser(member.userId) map { user ⇒ active(user, member.head, chan) }

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
        LineRepo.find(head.activeChanKeys, user.troll, _, NB_LINES) flatMap {
          _.map(namer.line).sequenceFu
        }
      }
    } map {
      case (namedChans, namedLines) ⇒ Chat(head, namedChans, namedLines.reverse)
    }

  def makeLine(chanKey: String, userId: String, t1: String): Fu[Option[Line]] =
    getUser(userId) flatMap { user ⇒
      val chanOption = Chan parse chanKey
      (chanOption match {
        case Some(TeamChan(teamId)) ⇒ getTeamIds(user.id) map (_ contains teamId)
        case _                      ⇒ fuccess(true)
      }) map {
        _ ?? {
          for {
            chan ← chanOption
            t2 ← Some(t1.trim take 200) filter (_.nonEmpty)
            if !user.disabled
          } yield Line.make(chan, user, Writer preprocessUserInput t2)
        }
      }
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

  private[chat] def getUser(userId: String) = getUserOption(userId) flatten s"No such user: $userId"

  private[chat] def getUserOption(userId: String) = UserRepo byId userId

  private val logger = play.api.Logger("chat")

  private object Writer {

    import java.util.regex.Matcher.quoteReplacement
    import org.apache.commons.lang3.StringEscapeUtils.escapeXml

    def preprocessUserInput(in: String) = addLinks(delocalize(noPrivateUrl(escapeXml(in))))

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
