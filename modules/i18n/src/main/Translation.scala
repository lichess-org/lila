package lila.i18n

import scalatags.Text.all.*

import lila.common.String.html.escapeHtml

sealed private trait Translation

final private class Simple(val message: String) extends Translation:

  def formatTxt(args: Seq[Any]): String =
    if args.isEmpty then message
    else message.format(args*)

  def format(args: Seq[RawFrag]): RawFrag =
    if args.isEmpty then RawFrag(message)
    else RawFrag(message.format(args.map(_.v)*))

  override def toString = s"Simple($message)"

final private class Escaped(val message: String, escaped: String) extends Translation:

  def formatTxt(args: Seq[Any]): String =
    if args.isEmpty then message
    else message.format(args*)

  def format(args: Seq[RawFrag]): RawFrag =
    if args.isEmpty then RawFrag(escaped)
    else RawFrag(escaped.format(args.map(_.v)*))

  override def toString = s"Escaped($message)"

final private class Plurals(val messages: Map[I18nQuantity, String]) extends Translation:

  private def messageFor(quantity: I18nQuantity): Option[String] =
    messages
      .get(quantity)
      .orElse(messages.get(I18nQuantity.Other))
      .orElse(messages.headOption._2F)

  def formatTxt(quantity: I18nQuantity, args: Seq[Any]): Option[String] =
    messageFor(quantity).map { message =>
      if args.isEmpty then message
      else message.format(args*)
    }

  def format(quantity: I18nQuantity, args: Seq[RawFrag]): Option[RawFrag] =
    messageFor(quantity).map { message =>
      val escaped = escapeHtml(Html(message))
      if args.isEmpty then escaped
      else RawFrag(escaped.v.format(args.map(_.v)*))
    }

  override def toString = s"Plurals($messages)"
