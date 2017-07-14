package lila.security

import com.roundeights.hasher.Algo
import lila.common.String.base64

import lila.user.{ User, UserRepo }
import UserTokener.ValueChecker

private[security] final class UserTokener(
    secret: String,
    getCurrentValue: User.ID => Fu[String],
    valueChecker: ValueChecker = ValueChecker.Same,
    fullHashSize: Int = 14,
    currentValueHashSize: Option[Int] = Some(6), // won't hash if None
    separator: Char = '|'
) {

  def make(user: User) = hashCurrentValue(user.id) map { hashedValue =>
    val payload = makePayload(user.id, hashedValue)
    val checksum = makeHash(payload)
    val token = s"$payload$separator$checksum"
    base64 encode token
  }

  def read(token: String): Fu[Option[User]] = (base64 decode token) ?? {
    _ split separator match {
      case Array(userId, hashed, checksum) if makeHash(makePayload(userId, hashed)) == checksum =>
        (valueChecker match {
          case ValueChecker.Same => hashCurrentValue(userId) map (hashed ==)
          case ValueChecker.Custom(f) => f(hashed)
        }) flatMap {
          _ ?? (UserRepo enabledById userId)
        }
      case _ => fuccess(none)
    }
  }

  private def makeHash(msg: String) = Algo.hmac(secret).sha1(msg).hex take fullHashSize

  private def hashCurrentValue(userId: User.ID) = getCurrentValue(userId) map { v =>
    currentValueHashSize.fold(v)(makeHash(v) take _)
  }

  private def makePayload(userId: User.ID, hashedValue: String) = s"$userId$separator$hashedValue"
}

private[security] object UserTokener {

  sealed trait ValueChecker
  object ValueChecker {
    case object Same extends ValueChecker
    case class Custom(f: String => Fu[Boolean]) extends ValueChecker
  }
}
