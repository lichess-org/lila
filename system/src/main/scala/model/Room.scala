package lila.system
package model

import com.novus.salat.annotations.Key
import com.mongodb.BasicDBList
import org.apache.commons.lang3.StringEscapeUtils.escapeXml
import collection.JavaConversions._

case class Room(
    @Key("_id") id: String,
    messages: List[BasicDBList]) {

  def render: String = messages map (_.toList) map {
    case author :: message :: Nil ⇒ Room.render(author.toString, message.toString)
    case _                        ⇒ ""
  } mkString
}

object Room {

  def render(author: String, message: String): String =
    """<li class="%s%s">%s</li>""".format(
      author,
      if (author == "system") " trans_me" else "",
      escapeXml(message))
}
