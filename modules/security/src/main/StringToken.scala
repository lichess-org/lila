package lila.security

import com.roundeights.hasher.Algo
import scalalib.Iso

import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest

import lila.common.String.base64
import lila.core.config.Secret

import StringToken.ValueChecker

final class StringToken[A](
    secret: Secret,
    getCurrentValue: A => Fu[String],
    valueChecker: ValueChecker = ValueChecker.Same,
    fullHashSize: Int = 14,
    currentValueHashSize: Option[Int] = Some(6), // won't hash if None
    separator: Char = '|'
)(using Executor)(using iso: Iso.StringIso[A]):

  def make(payload: A) =
    hashCurrentValue(payload).map { hashedValue =>
      val signed   = signPayload(iso to payload, hashedValue)
      val checksum = makeHash(signed)
      val token    = s"$signed$separator$checksum"
      base64.encode(token)
    }

  def read(token: String): Fu[Option[A]] =
    base64
      .decode(token)
      .so:
        _.split(separator) match
          case Array(payloadStr, hashed, checksum) =>
            MessageDigest
              .isEqual(
                makeHash(signPayload(payloadStr, hashed)).getBytes(UTF_8),
                checksum.getBytes(UTF_8)
              )
              .so:
                val payload = iso.from(payloadStr)
                valueChecker
                  .match
                    case ValueChecker.Same      => hashCurrentValue(payload).map(hashed ==)
                    case ValueChecker.Custom(f) => f(hashed)
                  .map { _.option(payload) }
          case _ => fuccess(none)

  private def makeHash(msg: String) = Algo.hmac(secret.value).sha256(msg).hex.take(fullHashSize)

  private def hashCurrentValue(payload: A) =
    getCurrentValue(payload).map: v =>
      currentValueHashSize.fold(v)(makeHash(v) take _)

  private def signPayload(payloadStr: String, hashedValue: String) = s"$payloadStr$separator$hashedValue"

object StringToken:

  enum ValueChecker:
    case Same
    case Custom(f: String => Fu[Boolean])

  object DateStr:
    def toStr(date: Instant)   = date.toMillis.toString
    def toInstant(str: String) = str.toLongOption.map(millisToInstant)

  def userId(secret: Secret, lifetime: FiniteDuration)(using Executor) = StringToken[UserId](
    secret = secret,
    getCurrentValue = _ => fuccess(StringToken.DateStr.toStr(nowInstant)),
    currentValueHashSize = none,
    valueChecker = StringToken.ValueChecker.Custom: v =>
      fuccess:
        StringToken.DateStr.toInstant(v).exists(nowInstant.minus(lifetime).isBefore)
  )
