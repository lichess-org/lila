package lila
package lobby

import db.{ MessageRepo, UserRepo }
import model.{ Message, User }

import scalaz.effects._
import org.apache.commons.lang3.StringEscapeUtils.escapeXml

final class Messenger(
    messageRepo: MessageRepo,
    userRepo: UserRepo) {

  private val urlRegex = """lichess\.org/([\w-]{8})[\w-]{4}""".r

  def apply(text: String, username: String): IO[Valid[Message]] = for {
    userOption ← userRepo byUsername username
    message = for {
      user ← userOption toValid "Unknown user"
      msg ← createMessage(text, user)
    } yield msg
    _ ← message.fold(err ⇒ io(failure(err)), messageRepo.add)
  } yield message

  def createMessage(text: String, user: User): Valid[Message] =
    if (user.isChatBan) !!("Chat banned " + user)
    else if (user.disabled) !!("User disabled " + user)
    else escapeXml(text.trim take 140) |> { escaped ⇒
      (escaped.nonEmpty).fold(
        success(Message(
          user.username,
          urlRegex.replaceAllIn(escaped, m ⇒ "lichess.org/" + (m group 1))
        )),
        !!("Empty message")
      )
    }

  def ban(username: String): IO[Unit] = for {
    userOption ← userRepo byUsername username
    _ ← userOption.fold(
      user ⇒ for {
        _ ← userRepo toggleChatBan user
        _ ← messageRepo deleteByUsername user.username
      } yield (),
      io()
    )
  } yield ()
}
