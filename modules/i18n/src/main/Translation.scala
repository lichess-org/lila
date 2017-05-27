package lila.i18n

import play.twirl.api.Html

import lila.common.String.html.{ escape => escapeHtml }

private sealed trait Translation extends Any

private case class Literal(message: String) extends AnyVal with Translation {

  private def escaped = escapeHtml(message)

  def formatTxt(args: Seq[Any]): String =
    if (args.isEmpty) message
    else message.format(args: _*)

  def formatHtml(args: Seq[Html]): Html =
    if (args.isEmpty) escaped
    else Html(escaped.body.format(args.map(_.body): _*))
}

private case class Plurals(messages: Map[I18nQuantity, String]) extends AnyVal with Translation {

  private def messageFor(quantity: I18nQuantity): Option[String] =
    messages.get(quantity)
      .orElse(messages.get(I18nQuantity.Other))
      .orElse(messages.headOption.map(_._2))

  def formatTxt(quantity: I18nQuantity, args: Seq[Any]): Option[String] =
    messageFor(quantity).map { message =>
      if (args.isEmpty) message
      else message.format(args: _*)
    }

  def formatHtml(quantity: I18nQuantity, args: Seq[Html]): Option[Html] =
    messageFor(quantity).map { message =>
      val escaped = escapeHtml(message)
      if (args.isEmpty) escaped
      else Html(escaped.body.format(args.map(_.body): _*))
    }
}
