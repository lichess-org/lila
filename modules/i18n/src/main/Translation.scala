package lila.i18n

import scalatags.Text.all._

import lila.common.String.html.escapeHtml

sealed private trait Translation

final private class Simple(val message: String) extends Translation {

  def formatTxt(args: Seq[Any]): String =
    if (args.isEmpty) message
    else message.format(args: _*)

  def format(args: Seq[RawFrag]): RawFrag =
    if (args.isEmpty) RawFrag(message)
    else RawFrag(message.format(args.map(_.v): _*))

  override def toString = s"Simple($message)"
}

final private class Escaped(val message: String, escaped: String) extends Translation {

  def formatTxt(args: Seq[Any]): String =
    if (args.isEmpty) message
    else message.format(args: _*)

  def format(args: Seq[RawFrag]): RawFrag =
    if (args.isEmpty) RawFrag(escaped)
    else RawFrag(escaped.format(args.map(_.v): _*))

  override def toString = s"Escaped($message)"
}

final private class Plurals(val messages: Map[I18nQuantity, String]) extends Translation {

  private def messageFor(quantity: I18nQuantity): Option[String] =
    messages
      .get(quantity)
      .orElse(messages.get(I18nQuantity.Other))
      .orElse(messages.headOption.map(_._2))

  def formatTxt(quantity: I18nQuantity, args: Seq[Any]): Option[String] =
    messageFor(quantity).map { message =>
      if (args.isEmpty) message
      else message.format(args: _*)
    }

  def format(quantity: I18nQuantity, args: Seq[RawFrag]): Option[RawFrag] =
    messageFor(quantity).map { message =>
      val escaped = escapeHtml(message)
      if (args.isEmpty) escaped
      else RawFrag(escaped.v.format(args.map(_.v): _*))
    }

  override def toString = s"Plurals($messages)"
}
