package lila.user
package ui

import lila.ui.ScalatagsTemplate.{ *, given }

final class userShow(helpers: lila.ui.Helpers):
  import helpers.*

  def transLocalize(key: lila.core.i18n.I18nKey, number: Int)(using Translate) =
    key.pluralSameTxt(number)

  def describeUser(user: lila.core.perf.UserWithPerfs)(using Translate) =
    import lila.rating.UserPerfsExt.bestRatedPerf
    val name      = user.titleUsername
    val nbGames   = user.count.game
    val createdAt = showEnglishDate(user.createdAt)
    val currentRating = user.perfs.bestRatedPerf.so: p =>
      s" Current ${p.key.perfTrans} rating: ${p.perf.intRating}."
    s"$name played $nbGames games since $createdAt.$currentRating"

  val i18nKeys = List(
    trans.site.youAreLeavingLichess,
    trans.site.neverTypeYourPassword,
    trans.site.cancel,
    trans.site.proceedToX
  )

  val dataUsername = attr("data-username")
