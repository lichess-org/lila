package lila.i18n

import play.api.i18n.Lang

object I18nDb {

  val all: Messages =
    lila.common.Chronometer.syncEffect(
      lila.i18n.db.Registry.load // .+("default.play" -> playDefaultMessages)
    ) { lap =>
      logger.info(s"${lap.millis}ms MessageDb")
    }

  val langs = all.keySet

  private def playDefaultMessages: Map[MessageKey, Translation] = Map(
    "constraint.required" -> Literal("Required"),
    "constraint.min" -> Literal("Minimum value: {0}"),
    "constraint.max" -> Literal("Maximum value: {0}"),
    "constraint.minLength" -> Literal("Minimum length: {0}"),
    "constraint.maxLength" -> Literal("Maximum length: {0}"),
    "constraint.email" -> Literal("Email"),
    "format.date" -> Literal("Date (''{0}'')"),
    "format.numeric" -> Literal("Numeric"),
    "format.real" -> Literal("Real"),
    "format.uuid" -> Literal("UUID"),
    "error.invalid" -> Literal("Invalid value"),
    "error.invalid.java.util.Date" -> Literal("Invalid date value"),
    "error.required" -> Literal("This field is required"),
    "error.number" -> Literal("Numeric value expected"),
    "error.real" -> Literal("Real number value expected"),
    "error.real.precision" -> Literal("Real number value with no more than {0} digit(s) including {1} decimal(s) expected"),
    "error.min" -> Literal("Must be greater or equal to {0}"),
    "error.min.strict" -> Literal("Must be strictly greater than {0}"),
    "error.max" -> Literal("Must be less or equal to {0}"),
    "error.max.strict" -> Literal("Must be strictly less than {0}"),
    "error.minLength" -> Literal("Minimum length is {0}"),
    "error.maxLength" -> Literal("Maximum length is {0}"),
    "error.email" -> Literal("Valid email required"),
    "error.pattern" -> Literal("Must satisfy {0}"),
    "error.date" -> Literal("Valid date required"),
    "error.uuid" -> Literal("Valid UUID required"),
    "error.expected.date" -> Literal("Date value expected"),
    "error.expected.date.isoformat" -> Literal("Iso date value expected"),
    "error.expected.time" -> Literal("Time value expected"),
    "error.expected.jodadate.format" -> Literal("Joda date value expected"),
    "error.expected.jodatime.format" -> Literal("Joda time value expected"),
    "error.expected.jsarray" -> Literal("Array value expected"),
    "error.expected.jsboolean" -> Literal("Boolean value expected"),
    "error.expected.jsnumber" -> Literal("Number value expected"),
    "error.expected.jsobject" -> Literal("Object value expected"),
    "error.expected.jsstring" -> Literal("String value expected"),
    "error.expected.jsnumberorjsstring" -> Literal("String or number expected"),
    "error.expected.keypathnode" -> Literal("Node value expected"),
    "error.expected.uuid" -> Literal("UUID value expected"),
    "error.expected.validenumvalue" -> Literal("Valid enumeration value expected"),
    "error.expected.enumstring" -> Literal("String value expected"),
    "error.path.empty" -> Literal("Empty path"),
    "error.path.missing" -> Literal("Missing path"),
    "error.path.result.multiple" -> Literal("Multiple results for the given path")
  )
}
