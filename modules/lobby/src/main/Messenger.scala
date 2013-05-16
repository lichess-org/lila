package lila.lobby

import lila.user.{ UserRepo, User, Room }
import tube.messageTube
import lila.db.api._
import lila.db.Implicits._
import lila.security.Flood

import play.api.libs.json.Json
import org.apache.commons.lang3.StringEscapeUtils.escapeXml

final class Messenger(
    flood: Flood,
    val netDomain: String) extends Room {

  def apply(userId: String, text: String): Fu[Message] = for {
    user ← UserRepo byId userId flatten "[lobby] messenger no such user"
    _ ← flood.allowMessage(userId, text).fold(funit, fufail("[lobby] flood detected"))
    message ← (userMessage(user.some, text) map {
      case (u, t) ⇒ Message.make(user.id.some, escapeXml(t), user.troll)
    }).future
    _ ← $insert(message)
  } yield message

  def system(text: String): Fu[Message] =
    Message.make(user = none, text = text, troll = false) |> { message ⇒
      $insert(message) inject message
    }

  def setTroll(username: String, v: Boolean): Funit =
    $update(Json.obj("user" -> username), $set("troll" -> v), multi = true)

  def recent(withTrolls: Boolean, limit: Int): Fu[List[Message]] =
    $query(withTrolls.fold(
      $select.all,
      Json.obj("troll" -> false)
    )) sort $sort.naturalOrder toListFlatten limit.some
}
