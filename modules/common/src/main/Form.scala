package lila.common

import chess.Color
import chess.format.Fen
import play.api.data.format.Formats.*
import play.api.data.format.Formatter
import play.api.data.Forms.*
import play.api.data.validation.{ Constraint, Constraints }
import play.api.data.{ Field, FormError, Mapping, Form as PlayForm }
import play.api.data.validation as V
import scala.util.Try
import java.time.LocalDate

object Form:

  type Options[A] = Iterable[(A, String)]

  type FormLike = {
    def apply(key: String): Field
    def errors: Seq[FormError]
  }

  def options(it: Iterable[Int], pattern: String): Options[Int] =
    it.map: d =>
      d -> (pluralize(pattern, d) format d)

  def options(it: Iterable[Int], transformer: Int => Int, pattern: String): Options[Int] =
    it.map: d =>
      d -> (pluralize(pattern, transformer(d)) format transformer(d))

  def options(it: Iterable[Int], code: String, pattern: String): Options[String] =
    it.map: d =>
      s"$d$code" -> (pluralize(pattern, d) format d)

  def options(it: Iterable[Int], format: Int => String): Options[Int] =
    it.map: d =>
      d -> format(d)

  def optionsDouble(it: Iterable[Double], format: Double => String): Options[Double] =
    it.map: d =>
      d -> format(d)

  def mustBeOneOf[A](choices: Iterable[A]) = s"Must be one of: ${choices mkString ", "}"

  def numberIn(choices: Options[Int]) =
    number.verifying(mustBeOneOf(choices.map(_._1)), hasKey(choices, _))

  def numberIn(choices: Set[Int]) =
    number.verifying(mustBeOneOf(choices), choices.contains)

  def numberIn(choices: Seq[Int]) =
    number.verifying(mustBeOneOf(choices), choices.contains)

  def numberInDouble(choices: Options[Double]) =
    of[Double].verifying(mustBeOneOf(choices.map(_._1)), hasKey(choices, _))

  def id[Id](size: Int, fixed: Option[Id])(exists: Id => Fu[Boolean])(using
      sr: StringRuntime[Id],
      rs: SameRuntime[String, Id]
  ): Mapping[Id] =
    val field = text(minLength = size, maxLength = size)
      .verifying("IDs must be made of ASCII letters and numbers", id => """(?i)^[a-z\d]+$""".r matches id)
      .into[Id]
    fixed match
      case Some(fixedId) => field.verifying("The ID cannot be changed now", id => id == fixedId)
      case None =>
        field.verifying("This ID is already in use", id => !exists(id).await(1.second, "unique ID"))

  def trim(m: Mapping[String]) = m.transform[String](_.trim, identity)

  // trims and removes garbage chars before validation
  private def makeCleanTextFormatter(keepSymbols: Boolean): Formatter[String] = new:
    def bind(key: String, data: Map[String, String]) =
      data
        .get(key)
        .map(if keepSymbols then String.softCleanUp else String.fullCleanUp)
        .toRight(Seq(FormError(key, "error.required", Nil)))
    def unbind(key: String, value: String) = Map(key -> String.normalize(value.trim))
  val cleanTextFormatter: Formatter[String]            = makeCleanTextFormatter(keepSymbols = false)
  val cleanTextFormatterWithSymbols: Formatter[String] = makeCleanTextFormatter(keepSymbols = true)

  val cleanText: Mapping[String]            = of(cleanTextFormatter)
  val cleanTextWithSymbols: Mapping[String] = of(cleanTextFormatterWithSymbols)

  def cleanText(minLength: Int = 0, maxLength: Int = Int.MaxValue): Mapping[String] =
    (minLength, maxLength) match
      case (min, Int.MaxValue) => cleanText.verifying(Constraints minLength min)
      case (0, max)            => cleanText.verifying(Constraints maxLength max)
      case (min, max)          => cleanText.verifying(Constraints minLength min, Constraints maxLength max)

  val cleanNonEmptyText: Mapping[String] = cleanText.verifying(Constraints.nonEmpty)
  def cleanNonEmptyText(minLength: Int = 0, maxLength: Int = Int.MaxValue): Mapping[String] =
    cleanText(minLength, maxLength).verifying(Constraints.nonEmpty)

  object eventName:

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

  object mustNotContainLichess:
    // \u0131\u0307 is ı (\u0131) with an i dot (\u0307)
    private val regex = "(?iu)l(?:[i\u0456]|\u0131\u0307?)[c\u0441][h\u04bb][e\u0435][s\u0455]".r
    def apply(verifiedUser: Boolean) = Constraint[String] { (t: String) =>
      if regex.find(t) && !verifiedUser then V.Invalid(V.ValidationError("Must not contain \"lichess\""))
      else V.Valid
    }

  def stringIn(choices: Options[String]) =
    text.verifying(mustBeOneOf(choices.map(_._1)), hasKey(choices, _))

  def stringIn(choices: Set[String]) =
    text.verifying(mustBeOneOf(choices), choices.contains)

  def typeIn[A: Formatter](choices: Set[A]) =
    of[A].verifying(mustBeOneOf(choices), choices.contains)

  def tolerantBoolean = of[Boolean](formatter.tolerantBooleanFormatter)

  def hasKey[A](choices: Options[A], key: A) =
    choices.map(_._1).toList contains key

  def trueish(v: Any) = v == 1 || v == "1" || v == "true" || v == "True" || v == "on" || v == "yes"

  def defaulting[A](m: Mapping[A], default: A) =
    optional(m).transform(_ | default, some)

  object color:
    val mapping: Mapping[Color] = trim(text)
      .verifying(Color.all.map(_.name).contains)
      .transform[Color](c => Color.fromWhite(c == "white"), _.name)

  private def pluralize(pattern: String, nb: Int) =
    pattern.replace("{s}", if nb == 1 then "" else "s")

  given intBase: Formatter[Int]      = intFormat
  given strBase: Formatter[String]   = stringFormat
  given boolBase: Formatter[Boolean] = booleanFormat

  object formatter:
    def string[A <: String](to: String => A): Formatter[A]                   = strBase.transform(to, identity)
    def stringFormatter[A](from: A => String, to: String => A): Formatter[A] = strBase.transform(to, from)
    def stringOptionFormatter[A](from: A => String, to: String => Option[A]): Formatter[A] =
      new Formatter[A]:
        def bind(key: String, data: Map[String, String]) = strBase.bind(key, data) flatMap { str =>
          to(str) toRight Seq(FormError(key, s"Invalid value: $str", Nil))
        }
        def unbind(key: String, value: A) = strBase.unbind(key, from(value))
    def int[A <: Int](to: Int => A): Formatter[A]                   = intBase.transform(to, identity)
    def intFormatter[A](from: A => Int, to: Int => A): Formatter[A] = intBase.transform(to, from)
    val tolerantBooleanFormatter: Formatter[Boolean] = new Formatter[Boolean]:
      override val format = Some(("format.boolean", Nil))
      def bind(key: String, data: Map[String, String]) =
        Right(data.getOrElse(key, "false")).flatMap { v =>
          Right(trueish(v))
        }
      def unbind(key: String, value: Boolean) = Map(key -> value.toString)

  object constraint:
    def minLength[A](from: A => String)(length: Int): Constraint[A] =
      Constraint[A]("constraint.minLength", length) { o =>
        if from(o).lengthIs >= length then V.Valid
        else V.Invalid(V.ValidationError("error.minLength", length))
      }
    def maxLength[A](from: A => String)(length: Int): Constraint[A] =
      Constraint[A]("constraint.maxLength", length) { o =>
        if from(o).lengthIs <= length then V.Valid
        else V.Invalid(V.ValidationError("error.maxLength", length))
      }

  object fen:
    val mapping = trim(of[String]).into[Fen.Epd]
    def playable(strict: Boolean) = mapping
      .verifying("Invalid position", fen => Fen.read(fen).exists(_ playable strict))
      .transform[Fen.Epd](if strict then truncateMoveNumber else identity, identity)
    val playableStrict = playable(strict = true)
    def truncateMoveNumber(fen: Fen.Epd) =
      Fen.readWithMoveNumber(fen).fold(fen) { g =>
        if g.fullMoveNumber >= 150 then
          Fen write g.copy(fullMoveNumber = g.fullMoveNumber.map(_ % 100)) // keep the start ply low
        else fen
      }

  object url:
    import io.mola.galimatias.{ StrictErrorHandler, URL, URLParsingSettings }
    private val parser = URLParsingSettings.create.withErrorHandler(StrictErrorHandler.getInstance)
    given Formatter[URL] with
      def bind(key: String, data: Map[String, String]) = stringFormat.bind(key, data) flatMap { url =>
        Try(URL.parse(parser, url)).fold(
          err => Left(Seq(FormError(key, s"Invalid URL: $err", Nil))),
          Right(_)
        )
      }
      def unbind(key: String, url: URL) = stringFormat.unbind(key, url.toString)

    val field = of[URL]

  given autoFormat[A, T](using
      sr: SameRuntime[A, T],
      rs: SameRuntime[T, A],
      base: Formatter[A]
  ): Formatter[T] with
    def bind(key: String, data: Map[String, String]) = base.bind(key, data) map sr.apply
    def unbind(key: String, value: T)                = base.unbind(key, rs(value))

  given Formatter[chess.variant.Variant] =
    import chess.variant.Variant
    formatter.stringFormatter[Variant](_.key.value, str => Variant.orDefault(Variant.LilaKey(str)))

  extension [A](f: Formatter[A])
    def transform[B](to: A => B, from: B => A): Formatter[B] = new:
      def bind(key: String, data: Map[String, String]) = f.bind(key, data) map to
      def unbind(key: String, value: B)                = f.unbind(key, from(value))
    def into[B](using sr: SameRuntime[A, B], rs: SameRuntime[B, A]): Formatter[B] =
      transform(sr.apply, rs.apply)

  extension [A](m: Mapping[A])
    def into[B](using sr: SameRuntime[A, B], rs: SameRuntime[B, A]): Mapping[B] =
      m.transform(sr.apply, rs.apply)

  extension [A](f: PlayForm[A]) def fillOption(o: Option[A]) = o.fold(f)(f.fill)

  object strings:
    def separator(sep: String) = of[List[String]]:
      formatter
        .stringFormatter[List[String]](_ mkString sep, _.split(sep).map(_.trim).toList.filter(_.nonEmpty))

  def inTheFuture(m: Mapping[Instant]) =
    m.verifying("The date must be set in the future", _.isAfterNow)

  object ISODate:
    val pattern                      = "yyyy-MM-dd"
    val format: Formatter[LocalDate] = localDateFormat(pattern)
    val mapping: Mapping[LocalDate]  = of[LocalDate](format)
  object ISODateTime:
    val format: Formatter[LocalDateTime] = new:
      val formatter                        = isoInstantFormatter
      def localDateTimeParse(data: String) = java.time.LocalDateTime.parse(data, formatter)
      def bind(key: String, data: Map[String, String]) =
        parsing(localDateTimeParse, "error.localDateTime", Nil)(key, data)
      def unbind(key: String, value: LocalDateTime) = Map(key -> formatter.print(value))
    val mapping: Mapping[LocalDateTime] = of[LocalDateTime](format)
  private object ISOInstant:
    val format: Formatter[Instant] = ISODateTime.format.transform(_.instant, _.dateTime)
    val mapping: Mapping[Instant]  = of[Instant](format)
  object PrettyDateTime:
    val pattern                          = "yyyy-MM-dd HH:mm"
    val format: Formatter[LocalDateTime] = localDateTimeFormat(pattern, utcZone)
    val mapping: Mapping[LocalDateTime]  = of[LocalDateTime](format)
  object Timestamp:
    val format: Formatter[Instant] = new:
      def bind(key: String, data: Map[String, String]) = {
        for
          str     <- stringFormat.bind(key, data)
          long    <- Try(java.lang.Long.parseLong(str)).toEither
          instant <- Try(millisToInstant(long)).toEither
        yield instant
      }.left.map(_ => Seq(FormError(key, "Invalid timestamp", Nil)))
      def unbind(key: String, value: Instant) = Map(key -> value.toMillis.toString)
    val mapping: Mapping[Instant] = of[Instant](format)
  object ISODateOrTimestamp:
    val format: Formatter[LocalDate] = new:
      def bind(key: String, data: Map[String, String]) =
        ISODate.format.bind(key, data) orElse Timestamp.format.bind(key, data).map(_.date)
      def unbind(key: String, value: LocalDate) = ISODate.format.unbind(key, value)
    val mapping = of[LocalDate](format)
  object ISOInstantOrTimestamp:
    val format: Formatter[Instant] = new:
      def bind(key: String, data: Map[String, String]) =
        ISOInstant.format.bind(key, data) orElse Timestamp.format.bind(key, data)
      def unbind(key: String, value: Instant) = ISOInstant.format.unbind(key, value)
    val mapping: Mapping[Instant] = of[Instant](format)
