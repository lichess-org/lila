package lila.security

import lila.user.{ User, UserRepo }

import com.roundeights.hasher.{ Hasher, Algo }
import play.api.libs.ws.{ WS, WSAuthScheme }
import play.api.Play.current

trait EmailConfirm {

  def effective: Boolean

  def send(user: User, email: String): Funit

  def confirm(token: String): Fu[Option[User]]
}

object EmailConfirmSkip extends EmailConfirm {

  def effective = false

  def send(user: User, email: String) = UserRepo setEmailConfirmed user.id

  def confirm(token: String): Fu[Option[User]] = fuccess(none)
}

final class EmailConfirmMailGun(
    apiUrl: String,
    apiKey: String,
    sender: String,
    baseUrl: String,
    secret: String) extends EmailConfirm {

  def effective = true

  def send(user: User, email: String): Funit = tokener make user flatMap { token =>
    lila.mon.email.confirmation()
    val url = s"$baseUrl/signup/confirm/$token"
    WS.url(s"$apiUrl/messages").withAuth("api", apiKey, WSAuthScheme.BASIC).post(Map(
      "from" -> Seq(sender),
      "to" -> Seq(email),
      "subject" -> Seq(s"Confirm your lichess.org account, ${user.username}"),
      "text" -> Seq(s"""
Final step!

Confirm your email address to complete your lichess account. It's easy — just click on the link below.

$url


Please do not reply to this message; it was sent from an unmonitored email address. This message is a service email related to your use of lichess.org.
"""))).void
  }

  def confirm(token: String): Fu[Option[User]] = tokener read token flatMap {
    case u@Some(user) => UserRepo setEmailConfirmed user.id inject u
    case _            => fuccess(none)
  }

  private object tokener {

    private val separator = '|'

    private def makeHash(msg: String) = Algo.hmac(secret).sha1(msg).hex take 14
    private def getHashedEmail(userId: User.ID) = UserRepo email userId map { p =>
      makeHash(~p) take 6
    }
    private def makePayload(userId: String, passwd: String) = s"$userId$separator$passwd"

    def make(user: User) = getHashedEmail(user.id) map { hashedEmail =>
      val payload = makePayload(user.id, hashedEmail)
      val hash = makeHash(payload)
      val token = s"$payload$separator$hash"
      base64 encode token
    }

    def read(token: String): Fu[Option[User]] = (base64 decode token) ?? {
      _ split separator match {
        case Array(userId, userHashedEmail, hash) if makeHash(makePayload(userId, userHashedEmail)) == hash =>
          getHashedEmail(userId) flatMap { hashedEmail =>
            (userHashedEmail == hashedEmail) ?? (UserRepo enabledById userId)
          }
        case _ => fuccess(none)
      }
    }
  }

  private object base64 {
    import java.util.Base64
    import java.nio.charset.StandardCharsets
    def encode(txt: String) =
      Base64.getEncoder.encodeToString(txt getBytes StandardCharsets.UTF_8)
    def decode(txt: String): Option[String] = try {
      Some(new String(Base64.getDecoder decode txt))
    }
    catch {
      case _: java.lang.IllegalArgumentException => none
    }
  }
}
