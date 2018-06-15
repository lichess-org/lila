package lila.i18n

import play.twirl.api.Html

import lila.common.String.html.escapeHtml

private sealed trait Translation

private final class Simple(val message: String) extends Translation {

  def formatTxt(args: Seq[Any]): String =
    if (args.isEmpty) message
    else message.format(args: _*)

  def formatHtml(args: Seq[Html]): Html =
    if (args.isEmpty) Html(message)
    else Html(message.format(args.map(_.body): _*))

  override def toString = s"Simple($message)"
}

private final class Escaped(val message: String, escaped: String) extends Translation {

  def formatTxt(args: Seq[Any]): String =
    if (args.isEmpty) message
    else message.format(args: _*)

  def formatHtml(args: Seq[Html]): Html =
    if (args.isEmpty) Html(escaped)
    else Html(escaped.format(args.map(_.body): _*))

  override def toString = s"Escaped($message)"
}

private final class Plurals(val messages: Map[I18nQuantity, String]) extends Translation {

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
