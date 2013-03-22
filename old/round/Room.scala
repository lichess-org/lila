package lila.app
package round

import com.novus.salat.annotations.Key
import org.apache.commons.lang3.StringEscapeUtils.escapeXml

case class Room(
    @Key("_id") id: String,
    messages: List[String]) {

  def render: String =
    messages map ((Room.render _) compose Room.decode) mkString ""

  def rematchCopy(id: String, nb: Int) = copy(
    id = id,
    messages = messages takeRight nb)

  def nonEmpty = messages.nonEmpty
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
}
