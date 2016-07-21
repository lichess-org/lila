package lila.security

import lila.user.{ User, UserRepo }

import com.roundeights.hasher.{ Hasher, Algo }
import play.api.libs.ws.{ WS, WSAuthScheme }
import play.api.Play.current

final class PasswordReset(
    apiUrl: String,
    apiKey: String,
    sender: String,
    baseUrl: String,
    secret: String) {

  def send(user: User, email: String): Funit = tokener make user flatMap { token =>
    lila.mon.email.resetPassword()
    val url = s"$baseUrl/password/reset/confirm/$token"
    WS.url(s"$apiUrl/messages").withAuth("api", apiKey, WSAuthScheme.BASIC).post(Map(
      "from" -> Seq(sender),
      "to" -> Seq(email),
      "subject" -> Seq("Reset your lichess.org password"),
      "text" -> Seq(s"""
We received a request to reset the password for your account, ${user.username}.

If you made this request, click the link below. If you didn't make this request, you can ignore this email.

$url


Please do not reply to this message; it was sent from an unmonitored email address. This message is a service email related to your use of lichess.org.
"""))).void addFailureEffect {
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

    def read(token: String): Fu[Option[User]] = base64 decode token split separator match {
      case Array(userId, userPass, hash) if makeHash(makePayload(userId, userPass)) == hash =>
        getPasswd(userId) flatMap { passwd =>
          (userPass == passwd) ?? (UserRepo enabledById userId)
        }
      case _ => fuccess(none)
    }
  }

  private object base64 {
    import java.util.Base64
    import java.nio.charset.StandardCharsets
    def encode(txt: String) =
      Base64.getEncoder.encodeToString(txt getBytes StandardCharsets.UTF_8)
    def decode(txt: String) =
      new String(Base64.getDecoder decode txt)
  }
}
