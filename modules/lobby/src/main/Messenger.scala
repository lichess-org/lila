package lila.lobby

import lila.user.{ UserRepo, User, Room }
import tube.messageTube
import lila.db.api._

import org.apache.commons.lang3.StringEscapeUtils.escapeXml

private[lobby] final class Messenger(val netDomain: String) extends Room {

  def apply(userId: String, text: String): Fu[Message] = for {
    userOption ← UserRepo byId userId
    message ← (userMessage(userOption, text) map {
      case (u, t) ⇒ Message.make(u.some, escapeXml(t))
    }).future
    _ ← $insert(message)
  } yield message

  def system(text: String): Fu[Message] =
    Message.make(user = none, text = text) |> { message ⇒
      $insert(message) inject message
    }

  def mute(username: String): Funit = MessageRepo censorUsername username

  def remove = MessageRepo removeRegex _
}
