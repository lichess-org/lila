package lila
package core

import user.User

import org.apache.commons.lang3.StringEscapeUtils.escapeXml

trait Room {

  def createMessage(user: User, text: String): Valid[(String, String)] =
    if (user.isChatBan) !!("Chat banned " + user)
    else if (user.disabled) !!("User disabled " + user)
    else escapeXml(text.replace(""""""", "'").trim take 140) |> { escaped ⇒
      (escaped.nonEmpty).fold(
        success((
          user.username,
          urlRegex.replaceAllIn(escaped, m ⇒ "lichess.org/" + (m group 1))
        )),
        !!("Empty message")
      )
    }

  private val urlRegex = """lichess\.org/([\w-]{8})[\w-]{4}""".r

  private def cleanupText(text: String) = {
    val cleanedUp = text.trim.replace(""""""", "'")
    (cleanedUp.size <= 140 && cleanedUp.nonEmpty) option cleanedUp
  }
}
