package lila.common

import play.api.i18n.Lang

object LangUtils {

  def isRTL(implicit lang: Lang): Boolean =
    lang.locale
      .getDisplayName(lang.locale)
      .charAt(0)
      .getDirectionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT || lang.locale
      .getDisplayName(lang.locale)
      .charAt(0)
      .getDirectionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC

}
