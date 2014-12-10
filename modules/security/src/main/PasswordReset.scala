package lila.security

import lila.user.User

import play.api.libs.ws.{ WS, WSAuthScheme }
import play.api.Play.current

final class PasswordReset(
    apiUrl: String,
    apiKey: String,
    sender: String,
    baseUrl: String) {

  def apply(user: User, email: String): Funit = {
    val url = s"$baseUrl/@/${user.username}"
    WS.url(s"$apiUrl/messages").withAuth("api", apiKey, WSAuthScheme.BASIC).post(Map(
      "from" -> Seq(sender),
      "to" -> Seq(email),
      "subject" -> Seq("Reset your lichess.org password"),
      "text" -> Seq(s"""
We received a request to reset the password for your account, ${user.username}.

If you made this request, click the link below. If you didn't make this request, you can ignore this email.

$url


Please do not reply to this message; it was sent from an unmonitored email address. This message is a service email related to your use of lichess.org.
""")))
  }.void
}
