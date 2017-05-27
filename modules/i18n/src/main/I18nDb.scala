package lila.i18n

import play.api.i18n.Lang

sealed trait Translation extends Any

case class Singular(message: String) extends AnyVal with Translation

case class Plurals(messages: Map[I18nQuantity, String]) extends AnyVal with Translation

object I18nDb {

  val all: Messages =
    lila.common.Chronometer.syncEffect(
      lila.i18n.db.Registry.load // .+("default.play" -> playDefaultMessages)
    ) { lap =>
      logger.info(s"${lap.millis}ms MessageDb")
    }

  val langs = all.keySet

  private def playDefaultMessages: Map[MessageKey, Translation] = Map(
    "constraint.required" -> Singular("Required"),
    "constraint.min" -> Singular("Minimum value: {0}"),
    "constraint.max" -> Singular("Maximum value: {0}"),
    "constraint.minLength" -> Singular("Minimum length: {0}"),
    "constraint.maxLength" -> Singular("Maximum length: {0}"),
    "constraint.email" -> Singular("Email"),
    "format.date" -> Singular("Date (''{0}'')"),
    "format.numeric" -> Singular("Numeric"),
    "format.real" -> Singular("Real"),
    "format.uuid" -> Singular("UUID"),
    "error.invalid" -> Singular("Invalid value"),
    "error.invalid.java.util.Date" -> Singular("Invalid date value"),
    "error.required" -> Singular("This field is required"),
    "error.number" -> Singular("Numeric value expected"),
    "error.real" -> Singular("Real number value expected"),
    "error.real.precision" -> Singular("Real number value with no more than {0} digit(s) including {1} decimal(s) expected"),
    "error.min" -> Singular("Must be greater or equal to {0}"),
    "error.min.strict" -> Singular("Must be strictly greater than {0}"),
    "error.max" -> Singular("Must be less or equal to {0}"),
    "error.max.strict" -> Singular("Must be strictly less than {0}"),
    "error.minLength" -> Singular("Minimum length is {0}"),
    "error.maxLength" -> Singular("Maximum length is {0}"),
    "error.email" -> Singular("Valid email required"),
    "error.pattern" -> Singular("Must satisfy {0}"),
    "error.date" -> Singular("Valid date required"),
    "error.uuid" -> Singular("Valid UUID required"),
    "error.expected.date" -> Singular("Date value expected"),
    "error.expected.date.isoformat" -> Singular("Iso date value expected"),
    "error.expected.time" -> Singular("Time value expected"),
    "error.expected.jodadate.format" -> Singular("Joda date value expected"),
    "error.expected.jodatime.format" -> Singular("Joda time value expected"),
    "error.expected.jsarray" -> Singular("Array value expected"),
    "error.expected.jsboolean" -> Singular("Boolean value expected"),
    "error.expected.jsnumber" -> Singular("Number value expected"),
    "error.expected.jsobject" -> Singular("Object value expected"),
    "error.expected.jsstring" -> Singular("String value expected"),
    "error.expected.jsnumberorjsstring" -> Singular("String or number expected"),
    "error.expected.keypathnode" -> Singular("Node value expected"),
    "error.expected.uuid" -> Singular("UUID value expected"),
    "error.expected.validenumvalue" -> Singular("Valid enumeration value expected"),
    "error.expected.enumstring" -> Singular("String value expected"),
    "error.path.empty" -> Singular("Empty path"),
    "error.path.missing" -> Singular("Missing path"),
    "error.path.result.multiple" -> Singular("Multiple results for the given path")
  )
}
