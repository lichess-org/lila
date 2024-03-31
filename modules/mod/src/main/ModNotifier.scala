package lila.mod

import lila.notify.NotifyApi
import lila.report.Suspect

final private class ModNotifier(
    notifyApi: NotifyApi,
    reportApi: lila.report.ReportApi
)(using Executor, lila.core.i18n.Translator):

  def reporters(mod: ModId, sus: Suspect): Funit =
    reportApi.recentReportersOf(sus).flatMap {
      _.filterNot(_.is(mod))
        .map: reporterId =>
          notifyApi.notifyOne(reporterId, lila.notify.ReportedBanned)
        .parallel
        .void
    }

  def refund(user: lila.user.User, pt: lila.rating.PerfType, points: Int): Funit =
    given play.api.i18n.Lang = user.realLang | lila.core.i18n.defaultLang
    notifyApi.notifyOne(user, lila.notify.RatingRefund(perf = pt.trans, points))
