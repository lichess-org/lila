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
    "constraint.required" -> new Literal("Required"),
    "constraint.min" -> new Literal("Minimum value: {0}"),
    "constraint.max" -> new Literal("Maximum value: {0}"),
    "constraint.minLength" -> new Literal("Minimum length: {0}"),
    "constraint.maxLength" -> new Literal("Maximum length: {0}"),
    "constraint.email" -> new Literal("Email"),
    "format.date" -> new Literal("Date (''{0}'')"),
    "format.numeric" -> new Literal("Numeric"),
    "format.real" -> new Literal("Real"),
    "format.uuid" -> new Literal("UUID"),
    "error.invalid" -> new Literal("Invalid value"),
    "error.invalid.java.util.Date" -> new Literal("Invalid date value"),
    "error.required" -> new Literal("This field is required"),
    "error.number" -> new Literal("Numeric value expected"),
    "error.real" -> new Literal("Real number value expected"),
    "error.real.precision" -> new Literal("Real number value with no more than {0} digit(s) including {1} decimal(s) expected"),
    "error.min" -> new Literal("Must be greater or equal to {0}"),
    "error.min.strict" -> new Literal("Must be strictly greater than {0}"),
    "error.max" -> new Literal("Must be less or equal to {0}"),
    "error.max.strict" -> new Literal("Must be strictly less than {0}"),
    "error.minLength" -> new Literal("Minimum length is {0}"),
    "error.maxLength" -> new Literal("Maximum length is {0}"),
    "error.email" -> new Literal("Valid email required"),
    "error.pattern" -> new Literal("Must satisfy {0}"),
    "error.date" -> new Literal("Valid date required"),
    "error.uuid" -> new Literal("Valid UUID required"),
    "error.expected.date" -> new Literal("Date value expected"),
    "error.expected.date.isoformat" -> new Literal("Iso date value expected"),
    "error.expected.time" -> new Literal("Time value expected"),
    "error.expected.jodadate.format" -> new Literal("Joda date value expected"),
    "error.expected.jodatime.format" -> new Literal("Joda time value expected"),
    "error.expected.jsarray" -> new Literal("Array value expected"),
    "error.expected.jsboolean" -> new Literal("Boolean value expected"),
    "error.expected.jsnumber" -> new Literal("Number value expected"),
    "error.expected.jsobject" -> new Literal("Object value expected"),
    "error.expected.jsstring" -> new Literal("String value expected"),
    "error.expected.jsnumberorjsstring" -> new Literal("String or number expected"),
    "error.expected.keypathnode" -> new Literal("Node value expected"),
    "error.expected.uuid" -> new Literal("UUID value expected"),
    "error.expected.validenumvalue" -> new Literal("Valid enumeration value expected"),
    "error.expected.enumstring" -> new Literal("String value expected"),
    "error.path.empty" -> new Literal("Empty path"),
    "error.path.missing" -> new Literal("Missing path"),
    "error.path.result.multiple" -> new Literal("Multiple results for the given path")
  )
}
