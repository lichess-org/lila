package lidraughts.message

import org.joda.time.DateTime

import lidraughts.shutup.Analyser
import lidraughts.user.User

private[message] final class MessageSecurity(
    follows: (String, String) => Fu[Boolean],
    blocks: (String, String) => Fu[Boolean],
    getPref: String => Fu[lidraughts.pref.Pref],
    spam: lidraughts.security.Spam
) {

  import lidraughts.pref.Pref.Message._

  def canMessage(from: String, to: String): Fu[Boolean] =
    blocks(to, from) flatMap {
      case true => fuFalse
      case false => getPref(to).map(_.message) flatMap {
        case NEVER => fuFalse
        case FRIEND => follows(to, from)
        case ALWAYS => fuTrue
      }
    }

  def muteThreadIfNecessary(thread: Thread, creator: User, invited: User): Fu[Thread] = {
    val fullText = s"${thread.name} ${~thread.firstPost.map(_.text)}"
    if (spam.detect(fullText)) {
      logger.warn(s"PM spam from ${creator.username}: fullText")
      fuTrue
    } else if (creator.troll) !follows(invited.id, creator.id)
    else if (Analyser(fullText).dirty && creator.createdAt.isAfter(DateTime.now.minusDays(30))) {
      follows(invited.id, creator.id) map { f =>
        if (!f) logger.warn(s"Mute dirty thread ${creator.username} -> ${invited.username} ${fullText.take(140)}")
        !f
      }
    } else fuFalse
  } map { mute =>
    if (mute) thread deleteFor invited
    else thread
  }
}
