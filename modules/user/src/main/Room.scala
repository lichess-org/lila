package lila.user

import java.util.regex.Matcher.quoteReplacement

trait Room {

  def netDomain: String

  def userMessage(userOption: Option[User], text: String): Valid[(String, String)] =
    userOption toValid "Anonymous cannot talk in this room" flatMap { user ⇒
      if (user.disabled) !!("User disabled " + user)
      // TODO troll
      // else if (user.isChatBan) !!("Chat banned " + user)
      else cleanupText(text) map { user.username -> _ }
    }

  def userOrAnonMessage(userOption: Option[User], text: String): Valid[(Option[String], String)] =
    cleanupText(text) map { userOption.map(_.username) -> _ }

  def cleanupText(text: String): Valid[String] =
    (text.replace(""""""", "'").trim take 140) |> { t ⇒
      if (t.isEmpty) !!("Empty message")
      else success(delocalize(noPrivateUrl(t)))
    }

  private def noPrivateUrl(str: String): String = 
    urlRegex.replaceAllIn(str, m ⇒ quoteReplacement(netDomain + "/" + (m group 1)))

  private val delocalize = new lila.common.String.Delocalizer(netDomain)
  private val domainRegex = netDomain.replace(".", """\.""")
  private val urlRegex = (domainRegex + """/([\w-]{8})[\w-]{4}""").r
}
