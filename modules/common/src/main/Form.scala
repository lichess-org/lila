package lila.common

import org.joda.time.DateTimeZone
import play.api.data.format.Formats._
import play.api.data.format.Formatter
import play.api.data.Forms._

object Form {

  type Options[A] = Iterable[(A, String)]

  def options(it: Iterable[Int], pattern: String): Options[Int] = it map { d =>
    d -> (pluralize(pattern, d) format d)
  }

  def options(it: Iterable[Int], transformer: Int => Int, pattern: String): Options[Int] = it map { d =>
    d -> (pluralize(pattern, transformer(d)) format transformer(d))
  }

  def options(it: Iterable[Int], code: String, pattern: String): Options[String] = it map { d =>
    (d + code) -> (pluralize(pattern, d) format d)
  }

  def optionsDouble(it: Iterable[Double], format: Double => String): Options[Double] = it map { d =>
    d -> format(d)
  }

  def numberIn(choices: Options[Int]) =
    number.verifying(hasKey(choices, _))

  def numberInDouble(choices: Options[Double]) =
    of[Double].verifying(hasKey(choices, _))

  def stringIn(choices: Options[String]) =
    text.verifying(hasKey(choices, _))

  def tolerantBoolean = of[Boolean](formatter.tolerantBooleanFormatter)

  def hasKey[A](choices: Options[A], key: A) =
    choices.map(_._1).toList contains key

  def trueish(v: Any) = v == 1 || v == "1" || v == "true" || v == "on" || v == "yes"

  private def pluralize(pattern: String, nb: Int) =
    pattern.replace("{s}", (nb != 1).fold("s", ""))

  object formatter {
    def stringFormatter[A](from: A => String, to: String => A): Formatter[A] = new Formatter[A] {
      def bind(key: String, data: Map[String, String]) = stringFormat.bind(key, data).right map to
      def unbind(key: String, value: A) = stringFormat.unbind(key, from(value))
    }
    def intFormatter[A](from: A => Int, to: Int => A): Formatter[A] = new Formatter[A] {
      def bind(key: String, data: Map[String, String]) = intFormat.bind(key, data).right map to
      def unbind(key: String, value: A) = intFormat.unbind(key, from(value))
    }
    val tolerantBooleanFormatter: Formatter[Boolean] = new Formatter[Boolean] {
      override val format = Some(("format.boolean", Nil))
      def bind(key: String, data: Map[String, String]) =
        Right(data.get(key).getOrElse("false")).right.flatMap { v =>
          Right(trueish(v))
        }
      def unbind(key: String, value: Boolean) = Map(key -> value.toString)
    }
  }

  object UTCDate {
    import play.api.data.format.JodaFormats._
    val dateTimePattern = "yyyy-MM-dd HH:mm"
    val utcDate = play.api.data.JodaForms.jodaDate(dateTimePattern, DateTimeZone.UTC)
    implicit val dateTimeFormat = jodaDateTimeFormat(dateTimePattern)
  }
  object ISODate {
    import play.api.data.format.JodaFormats._
    val dateTimePattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
    val isoDate = play.api.data.JodaForms.jodaDate(dateTimePattern, DateTimeZone.UTC)
    implicit val dateTimeFormat = jodaDateTimeFormat(dateTimePattern)
  }
}
