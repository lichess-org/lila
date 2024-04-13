package lila
package report

import lila.core.i18n.{ Translate, I18nKey as trans }

object ReportUi:

  def translatedReasonChoices(using Translate) =
    List(
      (Reason.Cheat.key, trans.site.cheat.txt()),
      (Reason.Comm.key, trans.site.insult.txt()),
      (Reason.Boost.key, trans.site.ratingManipulation.txt()),
      (Reason.Comm.key, trans.site.troll.txt()),
      (Reason.Sexism.key, "Sexual harassment or Sexist remarks"),
      (Reason.Username.key, trans.site.username.txt()),
      (Reason.Other.key, trans.site.other.txt())
    )
