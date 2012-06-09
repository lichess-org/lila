package lila
package round

import com.novus.salat.annotations.Key
import org.apache.commons.lang3.StringEscapeUtils.escapeXml
import scala.collection.JavaConversions._

case class WatcherRoom(
    @Key("_id") id: String,
    messages: List[String]) {

  def render: String =
    messages map ((WatcherRoom.render _) compose WatcherRoom.decode) mkString ""
}

object WatcherRoom {

  case class Message(author: String, text: String)

  def encode(author: String, text: String): String =
    author + "|" + text

  def decode(encoded: String): (String, String) =
    encoded.split("|").toList match {
      case author :: rest ⇒ (author, rest mkString "|")
      case Nil            ⇒ ("", "")
    }

  def render(msg: (String, String)): String = msg match {
    case (author, text) ⇒ """<li>%s%s</li>""".format(
      author,
      escapeXml(text)
    )
  }
}
