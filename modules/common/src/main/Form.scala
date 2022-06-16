package lila.common

import chess.Color
import chess.format.{ FEN, Forsyth }
import org.joda.time.{ DateTime, DateTimeZone }
import play.api.data.format.Formats._
import play.api.data.format.{ Formatter, JodaFormats }
import play.api.data.Forms._
import play.api.data.JodaForms._
import play.api.data.validation.{ Constraint, Constraints }
import play.api.data.{ Field, FormError, Mapping }
import play.api.data.{ validation => V }
import scala.util.Try

import lila.common.base.StringUtils

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

  private def mustBeOneOf(choices: Iterable[Any]) = s"Must be one of: ${choices mkString ", "}"

  def numberIn(choices: Options[Int]) =
    number.verifying(mustBeOneOf(choices.map(_._1)), hasKey(choices, _))

  def numberIn(choices: Set[Int]) =
    number.verifying(mustBeOneOf(choices), choices.contains _)

  def numberIn(choices: Seq[Int]) =
    number.verifying(mustBeOneOf(choices), choices.contains _)

  def numberInDouble(choices: Options[Double]) =
    of[Double].verifying(mustBeOneOf(choices.map(_._1)), hasKey(choices, _))

  def id(size: Int, fixed: Option[String])(exists: String => Fu[Boolean]) = {
    val field = text(minLength = size, maxLength = size)
      .verifying("IDs must be made of ASCII letters and numbers", id => """(?i)^[a-z\d]+$""".r matches id)
    fixed match {
      case Some(fixedId) => field.verifying("The ID cannot be changed now", id => id == fixedId)
      case None =>
        import scala.concurrent.duration._
        field.verifying(
          "This ID is already in use",
          id => !exists(id).await(1 second, "tour crud unique ID")
        )
    }
  }

  def trim(m: Mapping[String]) = m.transform[String](_.trim, identity)

  // trims and removes garbage chars before validation
  val cleanTextFormatter: Formatter[String] = new Formatter[String] {
    def bind(key: String, data: Map[String, String]) =
      data
        .get(key)
        .map(_.trim)
        .map(String.normalize.apply)
        .map(String.removeMultibyteSymbols)
        .toRight(Seq(FormError(key, "error.required", Nil)))
    def unbind(key: String, value: String) = Map(key -> String.normalize(value.trim))
  }

  val cleanText: Mapping[String] = of(cleanTextFormatter).verifying(
    V.Constraint((s: String) =>
      if (String.hasGarbageChars(s))
        V.Invalid(
          Seq(
            V.ValidationError(
              s"The text contains invalid chars: ${String.distinctGarbageChars(s) mkString " "}"
            )
          )
        )
      else V.Valid
    )
  )
  def cleanText(minLength: Int = 0, maxLength: Int = Int.MaxValue): Mapping[String] =
    (minLength, maxLength) match {
      case (min, Int.MaxValue) => cleanText.verifying(Constraints.minLength(min))
      case (0, max)            => cleanText.verifying(Constraints.maxLength(max))
      case (min, max)          => cleanText.verifying(Constraints.minLength(min), Constraints.maxLength(max))
    }

  val cleanNonEmptyText: Mapping[String] = cleanText.verifying(Constraints.nonEmpty)
  def cleanNonEmptyText(minLength: Int = 0, maxLength: Int = Int.MaxValue): Mapping[String] =
    cleanText(minLength, maxLength).verifying(Constraints.nonEmpty)

  object eventName {

    def apply(minLength: Int, maxLength: Int, verifiedUser: Boolean) =
      cleanText.verifying(
        Constraints minLength minLength,
        Constraints maxLength maxLength,
        Constraints.pattern(
          regex = """[\p{L}\p{N}-\s:.,;'°ª\+]+""".r,
          error = "Invalid characters; only letters, numbers, and common punctuation marks are accepted."
        ),
        mustNotContainLichess(verifiedUser)
      )
  }

  object mustNotContainLichess {
    private val regex = "(?iu)l[iıi̇][cс]h[eе]s".r
    def apply(verifiedUser: Boolean) = Constraint[String] { (t: String) =>
      if (regex.find(t) && !verifiedUser)
        V.Invalid(V.ValidationError("Must not contain \"lichess\""))
      else V.Valid
    }
  }

  def stringIn(choices: Options[String]) =
    text.verifying(mustBeOneOf(choices.map(_._1)), hasKey(choices, _))

  def stringIn(choices: Set[String]) =
    text.verifying(mustBeOneOf(choices), choices.contains _)

  def tolerantBoolean = of[Boolean](formatter.tolerantBooleanFormatter)

  def hasKey[A](choices: Options[A], key: A) =
    choices.map(_._1).toList contains key

  def trueish(v: Any) = v == 1 || v == "1" || v == "true" || v == "True" || v == "on" || v == "yes"

  object color {
    val mapping: Mapping[Color] = trim(text)
      .verifying(Color.all.map(_.name).contains _)
      .transform[Color](c => Color.fromWhite(c == "white"), _.name)
  }

  private def pluralize(pattern: String, nb: Int) =
    pattern.replace("{s}", if (nb == 1) "" else "s")

  object formatter {
    def stringFormatter[A](from: A => String, to: String => A): Formatter[A] =
      new Formatter[A] {
        def bind(key: String, data: Map[String, String]) = stringFormat.bind(key, data) map to
        def unbind(key: String, value: A)                = stringFormat.unbind(key, from(value))
      }
    def stringOptionFormatter[A](from: A => String, to: String => Option[A]): Formatter[A] =
      new Formatter[A] {
        def bind(key: String, data: Map[String, String]) = stringFormat.bind(key, data) flatMap { str =>
          to(str) toRight Seq(FormError(key, s"Invalid value: $str", Nil))
        }
        def unbind(key: String, value: A) = stringFormat.unbind(key, from(value))
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
    implicit val fenFormat = formatter.stringFormatter[FEN](_.value, FEN.apply)
    val playableStrict     = playable(strict = true)
    def playable(strict: Boolean) = of[FEN]
      .transform[FEN](f => FEN(f.value.trim), identity)
      .verifying("Invalid position", fen => (Forsyth <<< fen).exists(_.situation playable strict))
      .transform[FEN](if (strict) truncateMoveNumber else identity, identity)
    def truncateMoveNumber(fen: FEN) =
      (Forsyth <<< fen).fold(fen) { g =>
        if (g.fullMoveNumber >= 150)
          Forsyth >> g.copy(fullMoveNumber = g.fullMoveNumber % 100) // keep the start ply low
        else fen
      }
  }

  object url {
    import io.mola.galimatias.{ StrictErrorHandler, URL, URLParsingSettings }
    private val parser = URLParsingSettings.create.withErrorHandler(StrictErrorHandler.getInstance)
    implicit val urlFormat = new Formatter[URL] {
      def bind(key: String, data: Map[String, String]) = stringFormat.bind(key, data) flatMap { url =>
        Try(URL.parse(parser, url)).fold(
          err => Left(Seq(FormError(key, s"Invalid URL: $err", Nil))),
          Right(_)
        )
      }
      def unbind(key: String, url: URL) = stringFormat.unbind(key, url.toString)
    }
    val field = of[URL]
  }

  implicit val variantFormat =
    formatter.stringFormatter[chess.variant.Variant](_.key, chess.variant.Variant.orDefault)

  object strings {
    def separator(sep: String) = of[List[String]](
      formatter.stringFormatter[List[String]](_ mkString sep, _.split(sep).toList)
    )
  }

  def toMarkdown(m: Mapping[String]): Mapping[Markdown] = m.transform[Markdown](Markdown.apply, _.value)

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
