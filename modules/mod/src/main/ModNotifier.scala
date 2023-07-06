package lila.mod

import lila.notify.NotifyApi
import lila.report.{ Mod, Suspect, Victim }

final private class ModNotifier(
    notifyApi: NotifyApi,
    reportApi: lila.report.ReportApi
)(using Executor):

  def reporters(mod: ModId, sus: Suspect): Funit =
    reportApi.recentReportersOf(sus) flatMap {
      _.filterNot(_ is mod)
        .map: reporterId =>
          notifyApi.notifyOne(reporterId, lila.notify.ReportedBanned)
        .parallel
        .void
    }

  def refund(user: lila.user.User, pt: lila.rating.PerfType, points: Int): Funit =
    given play.api.i18n.Lang = user.realLang | lila.i18n.defaultLang
    notifyApi.notifyOne(user, lila.notify.RatingRefund(perf = pt.trans, points))
