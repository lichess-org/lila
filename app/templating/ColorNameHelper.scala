package lila.app
package templating

import lila.api.Context
import lila.pref.Pref

import lila.app.ui.ScalatagsTemplate._
import lila.i18n.I18nKey

trait ColorNameHelper { self: I18nHelper =>

  // maybe locale might be needed for some langauges?
  def transWithColorName(i18nKey: I18nKey, color: shogi.Color, isHandicap: Boolean)(implicit
      ctx: Context
  ): String =
    i18nKey
      .txt(
        if (isHandicap) handicapColorName(color)
        else standardColorName(color)
      )
      .toLowerCase
      .capitalize

  def standardColorName(color: shogi.Color)(implicit ctx: Context): String =
    ctx.pref.colorName match {
      case Pref.ColorName.SENTEJP => color.fold("先手", "後手")
      case Pref.ColorName.SENTE   => color.fold("Sente", "Gote")
      case Pref.ColorName.BLACK   => color.fold(trans.black.txt(), trans.white.txt())
      case _                      => color.fold(trans.sente.txt(), trans.gote.txt())
    }

  def handicapColorName(color: shogi.Color)(implicit ctx: Context): String =
    ctx.pref.colorName match {
      case Pref.ColorName.SENTEJP => color.fold("下手", "上手")
      case Pref.ColorName.SENTE   => color.fold("Shitate", "Uwate")
      case Pref.ColorName.BLACK   => color.fold(trans.black.txt(), trans.white.txt())
      case _                      => color.fold(trans.shitate.txt(), trans.uwate.txt())
    }

}
