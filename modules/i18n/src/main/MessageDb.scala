package lila.i18n

private object MessageDb {

  def load: Messages =
    lila.common.Chronometer.syncEffect(
      lila.i18n.db.Registry.load.+("default.play" -> playDefaultMessages)
    ) { lap =>
        logger.info(s"${lap.millis}ms MessageDb")
      }

  def playDefaultMessages: Map[String, String] = Map(
    "constraint.required" -> "Required",
    "constraint.min" -> "Minimum value: {0}",
    "constraint.max" -> "Maximum value: {0}",
    "constraint.minLength" -> "Minimum length: {0}",
    "constraint.maxLength" -> "Maximum length: {0}",
    "constraint.email" -> "Email",
    "format.date" -> "Date (''{0}'')",
    "format.numeric" -> "Numeric",
    "format.real" -> "Real",
    "format.uuid" -> "UUID",
    "error.invalid" -> "Invalid value",
    "error.invalid.java.util.Date" -> "Invalid date value",
    "error.required" -> "This field is required",
    "error.number" -> "Numeric value expected",
    "error.real" -> "Real number value expected",
    "error.real.precision" -> "Real number value with no more than {0} digit(s) including {1} decimal(s) expected",
    "error.min" -> "Must be greater or equal to {0}",
    "error.min.strict" -> "Must be strictly greater than {0}",
    "error.max" -> "Must be less or equal to {0}",
    "error.max.strict" -> "Must be strictly less than {0}",
    "error.minLength" -> "Minimum length is {0}",
    "error.maxLength" -> "Maximum length is {0}",
    "error.email" -> "Valid email required",
    "error.pattern" -> "Must satisfy {0}",
    "error.date" -> "Valid date required",
    "error.uuid" -> "Valid UUID required",
    "error.expected.date" -> "Date value expected",
    "error.expected.date.isoformat" -> "Iso date value expected",
    "error.expected.time" -> "Time value expected",
    "error.expected.jodadate.format" -> "Joda date value expected",
    "error.expected.jodatime.format" -> "Joda time value expected",
    "error.expected.jsarray" -> "Array value expected",
    "error.expected.jsboolean" -> "Boolean value expected",
    "error.expected.jsnumber" -> "Number value expected",
    "error.expected.jsobject" -> "Object value expected",
    "error.expected.jsstring" -> "String value expected",
    "error.expected.jsnumberorjsstring" -> "String or number expected",
    "error.expected.keypathnode" -> "Node value expected",
    "error.expected.uuid" -> "UUID value expected",
    "error.expected.validenumvalue" -> "Valid enumeration value expected",
    "error.expected.enumstring" -> "String value expected",
    "error.path.empty" -> "Empty path",
    "error.path.missing" -> "Missing path",
    "error.path.result.multiple" -> "Multiple results for the given path")
}
