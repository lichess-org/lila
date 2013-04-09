package lila.user

import org.apache.commons.lang3.StringEscapeUtils.escapeXml
import java.util.regex.Matcher.quoteReplacement

trait Room {

  def netDomain: String

  def createMessage(user: User, text: String): Valid[(String, String)] =
    if (user.isChatBan) !!("Chat banned " + user)
    else if (user.disabled) !!("User disabled " + user)
    else escapeXml(text.replace(""""""", "'").trim take 140) |> { escaped ⇒
      (escaped.nonEmpty).fold(
        success((
          user.username,
          urlRegex.replaceAllIn(escaped, m ⇒ quoteReplacement(netDomain + "/" + (m group 1)))
        )),
        !!("Empty message")
      )
    }

  private val domainRegex = netDomain.replace(".", """\.""")
  private val urlRegex = (domainRegex + """/([\w-]{8})[\w-]{4}""").r

  private def cleanupText(text: String) = {
    val cleanedUp = text.trim.replace(""""""", "'")
    (cleanedUp.size <= 140 && cleanedUp.nonEmpty) option cleanedUp
  }
}
