package lila.security

import java.nio.charset.StandardCharsets.UTF_8

import com.roundeights.hasher.Algo
import org.mindrot.BCrypt
import org.joda.time.DateTime

import lila.common.String.base64
import lila.common.config.Secret

import StringToken.ValueChecker

final class StringToken[A](
    secret: Secret,
    getCurrentValue: A => Fu[String],
    valueChecker: ValueChecker = ValueChecker.Same,
    fullHashSize: Int = 14,
    currentValueHashSize: Option[Int] = Some(6), // won't hash if None
    separator: Char = '|'
)(implicit
    ec: scala.concurrent.ExecutionContext,
    serializer: StringToken.Serializable[A]
) {

  def make(payload: A) =
    hashCurrentValue(payload) map { hashedValue =>
      val signed   = signPayload(serializer write payload, hashedValue)
      val checksum = makeHash(signed)
      val token    = s"$signed$separator$checksum"
      base64 encode token
    }

  def read(token: String): Fu[Option[A]] =
    (base64 decode token) ?? {
      _ split separator match {
        case Array(payloadStr, hashed, checksum) =>
          BCrypt.bytesEqualSecure(
            makeHash(signPayload(payloadStr, hashed)).getBytes(UTF_8),
            checksum.getBytes(UTF_8)
          ) ?? {
            val payload = serializer read payloadStr
            (valueChecker match {
              case ValueChecker.Same      => hashCurrentValue(payload) map (hashed ==)
              case ValueChecker.Custom(f) => f(hashed)
            }) map { _ option payload }
          }
        case _ => fuccess(none)
      }
    }

  private def makeHash(msg: String) = Algo.hmac(secret.value).sha256(msg).hex take fullHashSize

  private def hashCurrentValue(payload: A) =
    getCurrentValue(payload) map { v =>
      currentValueHashSize.fold(v)(makeHash(v) take _)
    }

  private def signPayload(payloadStr: String, hashedValue: String) = s"$payloadStr$separator$hashedValue"
}

object StringToken {

  trait Serializable[A] {
    def read(str: String): A
    def write(a: A): String
  }

  implicit final val stringSerializable = new Serializable[String] {
    def read(str: String) = str
    def write(a: String)  = a
  }

  sealed trait ValueChecker
  object ValueChecker {
    case object Same                            extends ValueChecker
    case class Custom(f: String => Fu[Boolean]) extends ValueChecker
  }

  object DateStr {
    def toStr(date: DateTime) = date.getMillis.toString
    def toDate(str: String)   = str.toLongOption map { new DateTime(_) }
  }
}
