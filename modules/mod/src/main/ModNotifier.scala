package lila.mod

import lila.core.notify.*
import lila.report.Suspect
import lila.core.perf.PerfType

final private class ModNotifier(
    notifyApi: NotifyApi,
    reportApi: lila.report.ReportApi
)(using Executor, lila.core.i18n.Translator):

  def reporters(mod: ModId, sus: Suspect): Funit =
    reportApi.recentReportersOf(sus).flatMap {
      _.filterNot(_.is(mod))
        .map: reporterId =>
          notifyApi.notifyOne(reporterId, ReportedBanned)
        .parallel
        .void
    }

  def refund(user: lila.user.User, pt: PerfType, points: Int): Funit =
    given play.api.i18n.Lang = user.realLang | lila.core.i18n.defaultLang
    notifyApi.notifyOne(user, RatingRefund(perf = pt.trans, points))
