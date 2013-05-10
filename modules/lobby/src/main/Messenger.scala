package lila.lobby

import lila.user.{ UserRepo, User, Room }
import lila.user.tube.userTube
import tube.messageTube
import lila.db.api._

private[lobby] final class Messenger(val netDomain: String) extends Room {

  def apply(userId: String, text: String): Fu[Message] = for {
    userOption ← $find.byId[User](userId)
    message ← (for {
      user ← userOption filter (_.canChat) toValid "This user cannot chat"
      msg ← createMessage(user, text)
      (u, t) = msg
    } yield Message.make(u, text)).future
    _ ← $insert(message)
  } yield message

  def system(text: String): Fu[Message] =
    Message.make(userId = "", text = text) |> { message ⇒
      $insert(message) inject message
    }

  def mute(username: String): Funit = MessageRepo censorUsername username

  def remove = MessageRepo removeRegex _
}
