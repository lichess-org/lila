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
    valueChecker: ValueChecker[A] = ValueChecker.Same[A](),
    fullHashSize: Int = 14,
    currentValueHashSize: Option[Int] = Some(10), // won't hash if None
    separator: Char = '|'
)(using Executor)(using iso: Iso.StringIso[A]):

  def make(payload: A) =
    hashCurrentValue(payload).map: hashedValue =>
      val signed = signPayload(iso to payload, hashedValue)
      val checksum = makeHash(signed)
      val token = s"$signed$separator$checksum"
      base64.encode(token)

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
                    case ValueChecker.Same() => hashCurrentValue(payload).map(hashed ==)
                    case ValueChecker.Custom(f) => f(payload, hashed)
                  .map { _.option(payload) }
          case _ => fuccess(none)

  private def makeHash(msg: String) = Algo.hmac(secret.value).sha256(msg).hex.take(fullHashSize)

  private def hashCurrentValue(payload: A) =
    getCurrentValue(payload).map: v =>
      currentValueHashSize.fold(v)(makeHash(v).take)

  private def signPayload(payloadStr: String, hashedValue: String) = s"$payloadStr$separator$hashedValue"

object StringToken:

  private type Hashed = String

  sealed trait ValueChecker[A]
  object ValueChecker:
    case class Same[A]() extends ValueChecker[A]
    case class Custom[A](f: (A, Hashed) => Fu[Boolean]) extends ValueChecker[A]

  object DateStr:
    def toStr(date: Instant) = date.toMillis.toString
    def toInstant(str: String) = str.toLongOption.map(millisToInstant)
    def valueCheck(str: String, lifetime: FiniteDuration) =
      toInstant(str).exists(nowInstant.minus(lifetime).isBefore)

  def withLifetime[A: Iso.StringIso](secret: Secret, lifetime: FiniteDuration)(using Executor) =
    StringToken[A](
      secret = secret,
      getCurrentValue = _ => fuccess(StringToken.DateStr.toStr(nowInstant)),
      currentValueHashSize = none,
      valueChecker = StringToken.ValueChecker.Custom: (_, hashed) =>
        fuccess(StringToken.DateStr.valueCheck(hashed, lifetime))
    )
  def withLifetimeAndFutureValue[A: Iso.StringIso](
      secret: Secret,
      lifetime: FiniteDuration,
      getCurrentValue: A => Fu[String]
  )(using Executor) =
    val separator = '/'
    StringToken[A](
      secret = secret,
      getCurrentValue = t =>
        getCurrentValue(t).map: v =>
          s"${StringToken.DateStr.toStr(nowInstant)}$separator$v",
      currentValueHashSize = None,
      valueChecker = ValueChecker.Custom[A]: (payload, hashed) =>
        hashed.split(separator) match
          case Array(dateStr, value) =>
            getCurrentValue(payload).map: cur =>
              cur == value && StringToken.DateStr.valueCheck(dateStr, lifetime)
          case _ => fuccess(false)
    )
