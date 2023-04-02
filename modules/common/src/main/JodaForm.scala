/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.data

import play.api.data.format.*

object JodaForms:
  import JodaFormats.{ *, given }

  val jodaDate: Mapping[org.joda.time.DateTime] = Forms.of[org.joda.time.DateTime]

  def jodaDate(
      pattern: String,
      timeZone: org.joda.time.DateTimeZone = org.joda.time.DateTimeZone.getDefault
  ): Mapping[org.joda.time.DateTime] =
    Forms.of[org.joda.time.DateTime].as(jodaDateTimeFormat(pattern, timeZone))

  /** Constructs a simple mapping for a date field (mapped as `org.joda.time.LocalDatetype`).
    *
    * For example:
    * {{{
    * Form("birthdate" -> jodaLocalDate)
    * }}}
    */
  val jodaLocalDate: Mapping[org.joda.time.LocalDate] = Forms.of[org.joda.time.LocalDate]

  /** Constructs a simple mapping for a date field (mapped as `org.joda.time.LocalDate type`).
    *
    * For example:
    * {{{
    * Form("birthdate" -> jodaLocalDate("dd-MM-yyyy"))
    * }}}
    *
    * @param pattern
    *   the date pattern, as defined in `org.joda.time.format.DateTimeFormat`
    */
  def jodaLocalDate(pattern: String): Mapping[org.joda.time.LocalDate] =
    Forms.of[org.joda.time.LocalDate].as(jodaLocalDateFormat(pattern))

object JodaFormats:

  /** Helper for formatters binders
    * @param parse
    *   Function parsing a String value into a T value, throwing an exception in case of failure
    * @param errArgs
    *   Error to set in case of parsing failure
    * @param key
    *   Key name of the field to parse
    * @param data
    *   Field data
    */
  private def parsing[T](parse: String => T, errMsg: String, errArgs: Seq[Matchable])(
      key: String,
      data: Map[String, String]
  ): Either[Seq[FormError], T] =
    Formats.stringFormat.bind(key, data).flatMap { s =>
      scala.util.control.Exception
        .allCatch[T]
        .either(parse(s))
        .left
        .map(_ => Seq(FormError(key, errMsg, errArgs)))
    }

  /** Formatter for the `org.joda.time.DateTime` type.
    *
    * @param pattern
    *   a date pattern as specified in `org.joda.time.format.DateTimeFormat`.
    * @param timeZone
    *   the `org.joda.time.DateTimeZone` to use for parsing and formatting
    */
  def jodaDateTimeFormat(
      pattern: String,
      timeZone: org.joda.time.DateTimeZone = org.joda.time.DateTimeZone.getDefault
  ): Formatter[org.joda.time.DateTime] = new:
    val formatter = org.joda.time.format.DateTimeFormat.forPattern(pattern).withZone(timeZone)

    override val format = Some(("format.date", Seq(pattern)))

    def bind(key: String, data: Map[String, String]) =
      parsing(formatter.parseDateTime, "error.date", Nil)(key, data)

    def unbind(key: String, value: org.joda.time.DateTime) = Map(
      key -> value.withZone(timeZone).toString(pattern)
    )

  given Formatter[org.joda.time.DateTime]  = jodaDateTimeFormat("yyyy-MM-dd")
  given Formatter[org.joda.time.LocalDate] = jodaLocalDateFormat("yyyy-MM-dd")

  /** Formatter for the `org.joda.time.LocalDate` type.
    *
    * @param pattern
    *   a date pattern as specified in `org.joda.time.format.DateTimeFormat`.
    */
  def jodaLocalDateFormat(pattern: String): Formatter[org.joda.time.LocalDate] = new:
    import org.joda.time.LocalDate

    val formatter                        = org.joda.time.format.DateTimeFormat.forPattern(pattern)
    def jodaLocalDateParse(data: String) = LocalDate.parse(data, formatter)

    override val format = Some(("format.date", Seq(pattern)))

    def bind(key: String, data: Map[String, String]) =
      parsing(jodaLocalDateParse, "error.date", Nil)(key, data)

    def unbind(key: String, value: LocalDate) = Map(key -> value.toString(pattern))
