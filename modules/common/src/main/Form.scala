package lila.common

import play.api.data.format.Formats._
import play.api.data.format.Formatter
import play.api.data.FormError
import play.api.data.Forms._
import play.api.libs.json._

object Form {

  def options(it: Iterable[Int], pattern: String) = it map { d =>
    d -> (pluralize(pattern, d) format d)
  }

  def options(it: Iterable[Int], transformer: Int => Int, pattern: String) = it map { d =>
    d -> (pluralize(pattern, transformer(d)) format transformer(d))
  }

  def options(it: Iterable[Int], code: String, pattern: String) = it map { d =>
    (d + code) -> (pluralize(pattern, d) format d)
  }

  def optionsDouble(it: Iterable[Double], format: Double => String) = it map { d =>
    d -> format(d)
  }

  def numberIn(choices: Iterable[(Int, String)]) =
    number.verifying(hasKey(choices, _))

  def numberInDouble(choices: Iterable[(Double, String)]) =
    of[Double].verifying(hasKey(choices, _))

  def stringIn(choices: Iterable[(String, String)]) =
    text.verifying(hasKey(choices, _))

  def hasKey[A](choices: Iterable[(A, _)], key: A) =
    choices.map(_._1).toList contains key

  private def pluralize(pattern: String, nb: Int) =
    pattern.replace("{s}", (nb != 1).fold("s", ""))

  private def pluralize(pattern: String, nb: Double) =
    pattern.replace("{s}", (nb < 1).fold("s", ""))

  private val jsonGlobalErrorRenamer = __.json update (
    (__ \ "global").json copyFrom (__ \ "").json.pick
  ) andThen (__ \ "").json.prune

  def errorsAsJson(form: play.api.data.Form[_])(implicit lang: play.api.i18n.Messages) =
    form.errorsAsJson validate jsonGlobalErrorRenamer getOrElse form.errorsAsJson

  object formatter {
    def stringFormatter[A](from: A => String, to: String => A): Formatter[A] = new Formatter[A] {
      def bind(key: String, data: Map[String, String]) = stringFormat.bind(key, data).right map to
      def unbind(key: String, value: A) = stringFormat.unbind(key, from(value))
    }
    def intFormatter[A](from: A => Int, to: Int => A): Formatter[A] = new Formatter[A] {
      def bind(key: String, data: Map[String, String]) = intFormat.bind(key, data).right map to
      def unbind(key: String, value: A) = intFormat.unbind(key, from(value))
    }
  }
}
