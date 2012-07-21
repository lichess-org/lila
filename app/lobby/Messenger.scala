package lila
package lobby

import user.{ UserRepo, User }

import scalaz.effects._
import org.apache.commons.lang3.StringEscapeUtils.escapeXml

final class Messenger(
    messageRepo: MessageRepo,
    userRepo: UserRepo) {

  private val urlRegex = """lichess\.org/([\w-]{8})[\w-]{4}""".r

  def apply(text: String, username: String): IO[Valid[Message]] = for {
    userOption ← userRepo byId username
    message = for {
      user ← userOption filter (_.canChat) toValid "This user cannot chat"
      msg ← createMessage(text, user)
    } yield msg
    _ ← message.fold(err ⇒ io(failure(err)), messageRepo.add)
  } yield message

  def createMessage(text: String, user: User): Valid[Message] =
    if (user.isChatBan) !!("Chat banned " + user)
    else if (user.disabled) !!("User disabled " + user)
    else escapeXml(text.replace(""""""", "'").trim take 140) |> { escaped ⇒
      (escaped.nonEmpty).fold(
        success(Message(
          username = user.username,
          text = urlRegex.replaceAllIn(escaped, m ⇒ "lichess.org/" + (m group 1))
        )),
        !!("Empty message")
      )
    }

  def mute(username: String): IO[Unit] = messageRepo censorUsername username
}
