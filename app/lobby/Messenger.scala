package lila
package lobby

import user.{ UserRepo, User }

import scalaz.effects._
import org.apache.commons.lang3.StringEscapeUtils.escapeXml

final class Messenger(
    messageRepo: MessageRepo,
    userRepo: UserRepo) extends core.Room {

  def apply(username: String, text: String): IO[Valid[Message]] = for {
    userOption ← userRepo byId username
    message = for {
      user ← userOption filter (_.canChat) toValid "This user cannot chat"
      msg ← createMessage(user, text)
      (u, t) = msg
    } yield Message(username = u, text = t)
    _ ← message.fold(_ ⇒ io(), messageRepo.add)
  } yield message

  def system(text: String): IO[Message] = Message(username = "", text = text) |> { message =>
    messageRepo add message inject message
  }

  def mute(username: String): IO[Unit] = messageRepo censorUsername username

  val remove = messageRepo.removeRegex _
}
