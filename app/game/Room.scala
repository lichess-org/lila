package lila
package game

import com.novus.salat.annotations.Key
import org.apache.commons.lang3.StringEscapeUtils.escapeXml
import scala.collection.JavaConversions._

case class Room(
    @Key("_id") id: String,
    messages: List[String]) {

  def render: String =
    messages map ((Room.render _) compose Room.decode) mkString ""
}

object Room {

  def encode(author: String, message: String): String = (author match {
    case "white" ⇒ "w"
    case "black" ⇒ "b"
    case _       ⇒ "s"
  }) + message

  def decode(encoded: String): (String, String) = (encoded take 1 match {
    case "w" ⇒ "white"
    case "b" ⇒ "black"
    case _   ⇒ "system"
  }, encoded drop 1)

  def render(msg: (String, String)): String = msg match {
    case (author, text) ⇒ """<li class="%s%s">%s</li>""".format(
      author, (author == "system").fold(" trans_me", ""), escapeXml(text)
    )
  }
}
