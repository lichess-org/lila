package lila.common

import chess.format.FEN
import chess.format.Forsyth
import org.joda.time.{ DateTime, DateTimeZone }
import play.api.data.format.Formats._
import play.api.data.format.{ Formatter, JodaFormats }
import play.api.data.Forms._
import play.api.data.JodaForms._
import play.api.data.validation.{ Constraint, Constraints }
import play.api.data.{ Field, FormError, Mapping }
import scala.util.Try

object Form {

  type Options[A] = Iterable[(A, String)]

  type FormLike = {
    def apply(key: String): Field
    def errors: Seq[FormError]
  }

  def options(it: Iterable[Int], pattern: String): Options[Int] =
    it map { d =>
      d -> (pluralize(pattern, d) format d)
    }

  def options(it: Iterable[Int], transformer: Int => Int, pattern: String): Options[Int] =
    it map { d =>
      d -> (pluralize(pattern, transformer(d)) format transformer(d))
    }

  def options(it: Iterable[Int], code: String, pattern: String): Options[String] =
    it map { d =>
      s"$d$code" -> (pluralize(pattern, d) format d)
    }

  def options(it: Iterable[Int], format: Int => String): Options[Int] =
    it map { d =>
      d -> format(d)
    }

  def optionsDouble(it: Iterable[Double], format: Double => String): Options[Double] =
    it map { d =>
      d -> format(d)
    }

  def numberIn(choices: Options[Int]) =
    number.verifying(hasKey(choices, _))

  def numberIn(choices: Set[Int]) =
    number.verifying(choices.contains _)

  def numberIn(choices: Seq[Int]) =
    number.verifying(choices.contains _)

  def numberInDouble(choices: Options[Double]) =
    of[Double].verifying(hasKey(choices, _))

  def trim(m: Mapping[String]) = m.transform[String](_.trim, identity)
  def clean(m: Mapping[String]) =
    trim(m)
      .verifying("This text contains invalid chars", s => !String.hasZeroWidthChars(s))

  def eventName(minLength: Int, maxLength: Int) =
    clean(text).verifying(
      Constraints minLength minLength,
      Constraints maxLength maxLength,
      Constraints.pattern(
        regex = """[\p{L}\p{N}-\s:.,;'\+]+""".r,
        error = "Invalid characters"
      )
    )

  def stringIn(choices: Options[String]) =
    text.verifying(hasKey(choices, _))

  def stringIn(choices: Set[String]) =
    text.verifying(choices.contains _)

  def tolerantBoolean = of[Boolean](formatter.tolerantBooleanFormatter)

  def hasKey[A](choices: Options[A], key: A) =
    choices.map(_._1).toList contains key

  def trueish(v: Any) = v == 1 || v == "1" || v == "true" || v == "on" || v == "yes"

  private def pluralize(pattern: String, nb: Int) =
    pattern.replace("{s}", if (nb == 1) "" else "s")

  object formatter {
    def stringFormatter[A](from: A => String, to: String => A): Formatter[A] =
      new Formatter[A] {
        def bind(key: String, data: Map[String, String]) = stringFormat.bind(key, data) map to
        def unbind(key: String, value: A)                = stringFormat.unbind(key, from(value))
      }
    def intFormatter[A](from: A => Int, to: Int => A): Formatter[A] =
      new Formatter[A] {
        def bind(key: String, data: Map[String, String]) = intFormat.bind(key, data) map to
        def unbind(key: String, value: A)                = intFormat.unbind(key, from(value))
      }
    val tolerantBooleanFormatter: Formatter[Boolean] = new Formatter[Boolean] {
      override val format = Some(("format.boolean", Nil))
      def bind(key: String, data: Map[String, String]) =
        Right(data.getOrElse(key, "false")).flatMap { v =>
          Right(trueish(v))
        }
      def unbind(key: String, value: Boolean) = Map(key -> value.toString)
    }
  }

  object constraint {
    import play.api.data.{ validation => V }
    def minLength[A](from: A => String)(length: Int): Constraint[A] =
      Constraint[A]("constraint.minLength", length) { o =>
        if (from(o).lengthIs >= length) V.Valid else V.Invalid(V.ValidationError("error.minLength", length))
      }
    def maxLength[A](from: A => String)(length: Int): Constraint[A] =
      Constraint[A]("constraint.maxLength", length) { o =>
        if (from(o).lengthIs <= length) V.Valid else V.Invalid(V.ValidationError("error.maxLength", length))
      }
  }

  object fen {
    implicit private val fenFormat = formatter.stringFormatter[FEN](_.value, FEN.apply)
    val playableStrict             = playable(strict = true)
    def playable(strict: Boolean) = of[FEN](fenFormat)
      .transform[FEN](f => FEN(f.value.trim), identity)
      .verifying("Invalid position", fen => (Forsyth <<< fen).exists(_.situation playable strict))
  }

  def inTheFuture(m: Mapping[DateTime]) =
    m.verifying(
      "The date must be set in the future",
      DateTime.now.isBefore(_)
    )

  object UTCDate {
    val dateTimePattern         = "yyyy-MM-dd HH:mm"
    val utcDate                 = jodaDate(dateTimePattern, DateTimeZone.UTC)
    implicit val dateTimeFormat = JodaFormats.jodaDateTimeFormat(dateTimePattern)
  }
  object ISODateTime {
    val dateTimePattern         = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
    val formatter               = JodaFormats.jodaDateTimeFormat(dateTimePattern, DateTimeZone.UTC)
    val isoDateTime             = jodaDate(dateTimePattern, DateTimeZone.UTC)
    implicit val dateTimeFormat = JodaFormats.jodaDateTimeFormat(dateTimePattern)
  }
  object ISODate {
    val datePattern         = "yyyy-MM-dd"
    val formatter           = JodaFormats.jodaDateTimeFormat(datePattern, DateTimeZone.UTC)
    val isoDateTime         = jodaDate(datePattern, DateTimeZone.UTC)
    implicit val dateFormat = JodaFormats.jodaDateTimeFormat(datePattern)
  }
  object Timestamp {
    val formatter = new Formatter[org.joda.time.DateTime] {
      def bind(key: String, data: Map[String, String]) =
        stringFormat
          .bind(key, data)
          .flatMap { str =>
            Try(java.lang.Long.parseLong(str)).toEither.flatMap { long =>
              Try(new DateTime(long)).toEither
            }
          }
          .left
          .map(_ => Seq(FormError(key, "Invalid timestamp", Nil)))
      def unbind(key: String, value: org.joda.time.DateTime) = Map(key -> value.getMillis.toString)
    }
    val timestamp = of[org.joda.time.DateTime](formatter)
  }
  object ISODateOrTimestamp {
    val formatter = new Formatter[org.joda.time.DateTime] {
      def bind(key: String, data: Map[String, String]) =
        ISODate.formatter.bind(key, data) orElse Timestamp.formatter.bind(key, data)
      def unbind(key: String, value: org.joda.time.DateTime) = ISODate.formatter.unbind(key, value)
    }
    val isoDateOrTimestamp = of[org.joda.time.DateTime](formatter)
  }
  object ISODateTimeOrTimestamp {
    val formatter = new Formatter[org.joda.time.DateTime] {
      def bind(key: String, data: Map[String, String]) =
        ISODateTime.formatter.bind(key, data) orElse Timestamp.formatter.bind(key, data)
      def unbind(key: String, value: org.joda.time.DateTime) = ISODateTime.formatter.unbind(key, value)
    }
    val isoDateTimeOrTimestamp = of[org.joda.time.DateTime](formatter)
  }
}
