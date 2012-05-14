package lila
package timeline

import game.DbGame

import com.novus.salat.annotations._
import com.mongodb.BasicDBList

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
      variant,
      rated ? "Rated" | "Casual",
      clock | "Unlimited")
}

object Entry {

  def apply(game: DbGame, encodedData: String): Option[Entry] =
    encodedData.split('$').toList match {
      case wu :: wue :: bu :: bue :: Nil ⇒ Some(
        new Entry(
          gameId = game.id,
          whiteName = wue,
          blackName = bue,
          whiteId = Some(wu) filter (_.nonEmpty),
          blackId = Some(bu) filter (_.nonEmpty),
          variant = game.variant.name,
          rated = game.isRated,
          clock = game.clock map (_.show))
      )
      case _ ⇒ None
    }
}
