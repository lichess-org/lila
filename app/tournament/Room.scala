package lila
package tournament

import controllers.routes.User.{ show ⇒ userRoute }
import com.novus.salat.annotations.Key
import org.apache.commons.lang3.StringEscapeUtils.escapeXml

case class Room(
    @Key("_id") id: String,
    messages: List[String]) {

  def render: String =
    messages map ((Room.render _) compose Room.decode) mkString ""

  def nonEmpty = messages.nonEmpty
}

object Room {

  case class Message(author: Option[String], text: String)

  def encode(msg: Message): String = (msg.author | "_") + " " + msg.text

  def decode(encoded: String): Message = encoded.takeWhile(' ' !=) match {
    case "_"  ⇒ Message(none, encoded.drop(2))
    case user ⇒ Message(user.some, encoded.drop(user.size + 1))
  }

  def render(msg: Message): String =
    """<li><span>%s</span>%s</li>""".format(
      msg.author.fold(
        u ⇒ """<a class="user_link" href="%s">%s</a>""".format(
          userRoute(u), u take 12
        ),
        """<span class="system"></span>"""
      ),
      escapeXml(msg.text)
    )
}
