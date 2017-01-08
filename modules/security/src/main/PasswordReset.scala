package lila.security

import com.roundeights.hasher.{ Hasher, Algo }
import play.api.libs.ws.{ WS, WSAuthScheme }
import play.api.Play.current

import lila.common.String.base64
import lila.user.{ User, UserRepo }

final class PasswordReset(
    apiUrl: String,
    apiKey: String,
    sender: String,
    replyTo: String,
    baseUrl: String,
    secret: String) {

  def send(user: User, email: String): Funit = tokener make user flatMap { token =>
    lila.mon.email.resetPassword()
    val url = s"$baseUrl/password/reset/confirm/$token"
    WS.url(s"$apiUrl/messages").withAuth("api", apiKey, WSAuthScheme.BASIC).post(Map(
      "from" -> Seq(sender),
      "to" -> Seq(email),
      "h:Reply-To" -> Seq(replyTo),
      "o:tag" -> Seq("password"),
      "subject" -> Seq("Reset your lichess.org password"),
      "text" -> Seq(s"""
We received a request to reset the password for your account, ${user.username}.

If you made this request, click the link below. If you didn't make this request, you can ignore this email.

$url


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
      </div>
      <div itemprop="publisher" itemscope itemtype="http://schema.org/Organization">
        <small>This is a service email related to your use of <a itemprop="url" href="https://lichess.org/"><span itemprop="name">lichess.org</span></a>.</small>
      </div>
    </div>
  </body>
</html>"""))).void addFailureEffect {
      case e: java.net.ConnectException => lila.mon.http.mailgun.timeout()
      case _                            =>
    }
  }

  def confirm(token: String): Fu[Option[User]] = tokener read token

  private object tokener {

    private val separator = '|'

    private def makeHash(msg: String) = Algo.hmac(secret).sha1(msg).hex take 14
    private def getPasswd(userId: User.ID) = UserRepo getPasswordHash userId map { p =>
      makeHash(~p) take 6
    }
    private def makePayload(userId: String, passwd: String) = s"$userId$separator$passwd"

    def make(user: User) = getPasswd(user.id) map { passwd =>
      val payload = makePayload(user.id, passwd)
      val hash = makeHash(payload)
      val token = s"$payload$separator$hash"
      base64 encode token
    }

    def read(token: String): Fu[Option[User]] = (base64 decode token) ?? {
      _ split separator match {
        case Array(userId, userPass, hash) if makeHash(makePayload(userId, userPass)) == hash =>
          getPasswd(userId) flatMap { passwd =>
            (userPass == passwd) ?? (UserRepo enabledById userId)
          }
        case _ => fuccess(none)
      }
    }
  }
}
