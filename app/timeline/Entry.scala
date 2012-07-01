package lila
package timeline

import com.novus.salat.annotations._

case class Entry(
    gameId: String,
    whiteName: String,
    blackName: String,
    whiteId: Option[String],
    blackId: Option[String],
    variant: String,
    rated: Boolean,
    clock: Option[String]) {

  def players: List[(String, Option[String])] = List(
    whiteName -> whiteId,
    blackName -> blackId)

  def render =
    "<td>%s</td><td>%s</td><td class='trans_me'>%s</td><td class='trans_me'>%s</td><td class='trans_me'>%s</td>".format(
      "<a class='watch' href='/%s'></a>" format gameId,
      players map {
        case (name, None) ⇒ name
        case (name, Some(id)) ⇒
          "<a class='user_link' href='/@/%s'>%s</a>".format(id, name)
      } mkString " vs ",
      variant.capitalize,
      rated ? "Rated" | "Casual",
      clock | "Unlimited")
}
