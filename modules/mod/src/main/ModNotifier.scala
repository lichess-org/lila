package lila.mod

import lila.notify.{ Notification, NotifyApi }
import lila.report.{ Mod, Suspect, Victim }

final private class ModNotifier(
    notifyApi: NotifyApi,
    reportApi: lila.report.ReportApi
)(using scala.concurrent.ExecutionContext):

  def reporters(mod: Mod, sus: Suspect): Funit =
    reportApi.recentReportersOf(sus) flatMap {
      _.filterNot(mod.user is _)
        .map { reporterId =>
          notifyApi.addNotification(
            Notification.make(
              notifies = UserId(reporterId.value),
              content = lila.notify.ReportedBanned
            )
          )
        }
        .sequenceFu
        .void
    }

  def refund(victim: Victim, pt: lila.rating.PerfType, points: Int): Funit =
    notifyApi.addNotification {
      given play.api.i18n.Lang = victim.user.realLang | lila.i18n.defaultLang
      Notification.make(
        notifies = victim.user.id,
        content = lila.notify.RatingRefund(pt.trans, points)
      )
    }.void
