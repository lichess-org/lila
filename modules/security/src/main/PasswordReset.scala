package lila.security

import com.roundeights.hasher.Algo
import play.api.libs.ws.{ WS, WSAuthScheme }
import play.api.Play.current

import lila.common.String.base64
import lila.common.EmailAddress
import lila.user.{ User, UserRepo }

final class PasswordReset(
    apiUrl: String,
    apiKey: String,
    sender: String,
    replyTo: String,
    baseUrl: String,
    secret: String
) {

  def send(user: User, email: EmailAddress): Funit = tokener make user flatMap { token =>
    lila.mon.email.resetPassword()
    val url = s"$baseUrl/password/reset/confirm/$token"
    WS.url(s"$apiUrl/messages").withAuth("api", apiKey, WSAuthScheme.BASIC).post(Map(
      "from" -> Seq(sender),
      "to" -> Seq(email.value),
      "h:Reply-To" -> Seq(replyTo),
      "o:tag" -> Seq("password"),
      "subject" -> Seq("Reset your lichess.org password"),
      "text" -> Seq(s"""
We received a request to reset the password for your account, ${user.username}.

If you made this request, click the link below. If you didn't make this request, you can ignore this email.

$url

(Clicking not working? Try pasting it into your browser!)

This message is a service email related to your use of lichess.org.
"""),
      "html" -> Seq(s"""<!doctype html>
<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <meta name="viewport" content="width=device-width" />
    <title>Reset your lichess.org password</title>
  </head>
  <body>
    <div itemscope itemtype="http://schema.org/EmailMessage">
      <p itemprop="description">We received a request to reset the password for your account, ${user.username}.</p>
      <p>If you made this request, click the link below. If you didn't make this request, you can ignore this email.</p>
      <div itemprop="potentialAction" itemscope itemtype="http://schema.org/ViewAction">
        <meta itemprop="name" content="Reset password">
        <meta itemprop="url" content="$url">
        <p><a itemprop="target" href="$url">$url</a></p>
        <p>(Clicking not working? Try pasting it into your browser!)</p>
      </div>
      <div itemprop="publisher" itemscope itemtype="http://schema.org/Organization">
        <small>This is a service email related to your use of <a itemprop="url" href="https://lichess.org/"><span itemprop="name">lichess.org</span></a>.</small>
      </div>
    </div>
  </body>
</html>""")
    )).void addFailureEffect {
      case e: java.net.ConnectException => lila.mon.http.mailgun.timeout()
      case _ =>
    }
  }

  def confirm(token: String): Fu[Option[User]] = tokener read token

  private val tokener = new UserTokener(
    secret = secret,
    getCurrentValue = id => UserRepo getPasswordHash id map (~_)
  )
}
