package lila.security

import com.roundeights.hasher.Algo
import org.joda.time.DateTime

import lila.common.String.base64
import lila.user.{ User, UserRepo }

final class LoginToken(secret: String) {

  def generate(user: User): String = tokener make user

  def consume(token: String): Fu[Option[User]] = tokener read token

  private object tokener {

    private val separator = '|'

    private def makeHash(msg: String) = Algo.hmac(secret).sha1(msg).hex take 14
    private def makePayload(userId: String, milliStr: String) = s"$userId$separator$milliStr"

    private object DateStr {
      def toStr(date: DateTime) = date.getMillis.toString
      def toDate(str: String) = parseLongOption(str) map { new DateTime(_) }
    }

    def make(user: User) = {
      val payload = makePayload(user.id, DateStr.toStr(DateTime.now))
      val hash = makeHash(payload)
      val token = s"$payload$separator$hash"
      base64 encode token
    }

    def read(token: String): Fu[Option[User]] = (base64 decode token) ?? {
      _ split separator match {
        case Array(userId, milliStr, hash) if makeHash(makePayload(userId, milliStr)) == hash =>
          DateStr.toDate(milliStr).exists(DateTime.now.minusMinutes(1).isBefore) ?? {
            UserRepo enabledById userId
          }
        case _ => fuccess(none)
      }
    }
  }
}
