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

  def apply(userId: String, text: String): Fu[Option[Message]] =
    UserRepo byId userId flatten "[lobby] messenger no such user" flatMap { user ⇒
      if (flood.allowMessage(userId, text)) for {
        message ← (userMessage(user.some, text) map {
          case (u, t) ⇒ Message.make(user.id.some, escapeXml(t), user.troll)
        }).future
        _ ← $insert(message)
      } yield message.some
      else fuloginfo("[lobby] %s is flooding the lobby room" format userId) inject none
    }

  def system(text: String): Fu[Message] =
    Message.make(user = none, text = text, troll = false) |> { message ⇒
      $insert(message) inject message
    }

  def recent(withTrolls: Boolean, limit: Int): Fu[List[Message]] =
    $query(withTrolls.fold(
      $select.all,
      Json.obj("troll" -> false)
    )) sort $sort.naturalOrder toListFlatten limit.some
}
