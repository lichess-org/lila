package lila.mod

import lila.core.notify.{ NotifyApi, NotificationContent }
import lila.core.report.SuspectId
import lila.rating.PerfType
import lila.report.{ Room, Suspect, ReporterId }
import lila.core.msg.SystemMsg

final private class ModNotifier(
    notifyApi: NotifyApi,
    reportApi: lila.report.ReportApi,
    pmPresets: ModPresetsApi,
    msgApi: lila.core.msg.MsgApi
)(using Executor, lila.core.i18n.Translator):

  object actionTaken:
    private val onceEvery = scalalib.cache.OnceEvery[(SuspectId, UserId)](1.hour)
    private val ignore = Set(ReporterId.lichess, ReporterId.irwin, ReporterId.kaladin)

    def apply(mod: ModId, sus: Suspect, room: Room): Funit =
      reportApi
        .recentReportersOf(sus, room)
        .flatMap:
          _.filterNot(ignore)
            .filterNot(_.is(mod))
            .sequentiallyVoid: reporterId =>
              onceEvery(sus.id -> reporterId.id).so:
                msgApi.systemPost(SystemMsg.standard(reporterId.userId, actionTakenMessage)).void

  def refund(user: User, pt: PerfType, points: Int): Funit =
    given play.api.i18n.Lang = user.realLang | lila.core.i18n.defaultLang
    notifyApi.notifyOne(user, NotificationContent.RatingRefund(perf = pt.trans, points))

  def notifyKidMode(mod: ModId, user: User): Funit =
    pmPresets.setKidModePreset match
      case None =>
        msgApi
          .systemPost:
            SystemMsg.standard(mod.userId, "No kid mode preset found, couldn't send a PM.")
          .void
      case Some(preset) => msgApi.systemPost(SystemMsg.mustRead(user.id, preset.text)).void

  private val actionTakenMessage = """Hello,

We have reviewed your recent report and taken action. While we cannot share details about the actions taken, we appreciate your report.

Thank you for your help in keeping Lichess a good place for everyone."""
