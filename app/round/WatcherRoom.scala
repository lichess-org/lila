package lila.app
package round

import controllers.routes.User.{ show ⇒ userRoute }

import com.novus.salat.annotations.Key
import org.apache.commons.lang3.StringEscapeUtils.escapeXml
import scala.collection.JavaConversions._

case class WatcherRoom(
    @Key("_id") id: String,
    messages: List[String]) {

  def render: String =
    messages map ((WatcherRoom.render _) compose WatcherRoom.decode) mkString ""

  def rematchCopy(id: String, nb: Int) = copy(
    id = id,
    messages = messages takeRight nb)

  def nonEmpty = messages.nonEmpty
}

object WatcherRoom {

  case class Message(username: Option[String], text: String)

  def encode(message: Message): String =
    (message.username | "") + "|" + message.text

  def decode(encoded: String): Message =
    encoded.split("\\|").toList match {
      case "" :: rest       ⇒ Message(None, rest mkString "|")
      case username :: rest ⇒ Message(Some(username), rest mkString "|")
      case Nil              ⇒ Message(None, "")
    }

  def render(msg: Message): String =
    """<li><span>%s</span>%s</li>""".format(
      msg.username.fold("Anonymous") { u ⇒
        """<a class="user_link" href="%s">%s</a>""".format(userRoute(u), u take 12)
      },
      escapeXml(msg.text)
    )
}
