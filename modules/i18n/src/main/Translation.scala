package lila.i18n

import play.twirl.api.Html

import lila.common.String.html.escapeHtml

private sealed trait Translation

private class Literal(val message: String, escapedOption: Option[String]) extends Translation {

  @inline private def escaped = escapedOption getOrElse message

  def formatTxt(args: Seq[Any]): String =
    if (args.isEmpty) message
    else message.format(args: _*)

  def formatHtml(args: Seq[Html]): Html =
    if (args.isEmpty) Html(escaped)
    else Html(escaped.format(args.map(_.body): _*))

  override def toString = s"Literal($message)"
}

private class Plurals(val messages: Map[I18nQuantity, String]) extends Translation {

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

  override def toString = s"Plurals($messages)"
}
