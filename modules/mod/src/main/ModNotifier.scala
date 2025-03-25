package lila.mod

import lila.core.notify.{ NotifyApi, NotificationContent }
import lila.rating.PerfType
import lila.report.Suspect

final private class ModNotifier(
    notifyApi: NotifyApi,
    reportApi: lila.report.ReportApi
)(using Executor, lila.core.i18n.Translator):

  def reporters(mod: ModId, sus: Suspect): Funit =
    reportApi
      .recentReportersOf(sus)
      .flatMap:
        _.filterNot(_.is(mod))
          .parallelVoid: reporterId =>
            notifyApi.notifyOne(reporterId, NotificationContent.ReportedBanned)

  def refund(user: User, pt: PerfType, points: Int): Funit =
    given play.api.i18n.Lang = user.realLang | lila.core.i18n.defaultLang
    notifyApi.notifyOne(user, NotificationContent.RatingRefund(perf = pt.trans, points))
