package lila.mod

import play.api.i18n.Lang

import lila.core.notify.{ NotifyApi, NotificationContent }
import lila.core.report.SuspectId
import lila.rating.PerfType
import lila.report.{ Room, Suspect, ReporterId }
import lila.core.msg.SystemMsg
import lila.core.i18n.{ I18nKey, Translate, Translator, LangPicker }

final private class ModNotifier(
    notifyApi: NotifyApi,
    reportApi: lila.report.ReportApi,
    pmPresets: ModPresetsApi,
    msgApi: lila.core.msg.MsgApi,
    userApi: lila.core.user.UserApi,
    langPicker: LangPicker
)(using Executor, Translator):

  object actionTaken:
    private val onceEvery = scalalib.cache.OnceEvery[(SuspectId, UserId)](1.hour)
    private val ignore = Set(ReporterId.lichess, ReporterId.irwin, ReporterId.kaladin)

    def apply(mod: ModId, sus: Suspect, room: Room): Funit =
      reportApi
        .recentReportersOf(sus, room)
        .flatMap:
          _.filterNot(ignore)
            .filterNot(_.is(mod))
            .sequentiallyVoid: reporter =>
              onceEvery(sus.id -> reporter.id).so:
                for
                  lang <- userApi.langOf(reporter.id).map(langPicker.byLangTagOrDefault)
                  msg = actionTakenMessage(using lang)
                  _ <- msgApi.systemPost(SystemMsg.standard(reporter.id, msg))
                yield ()

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

  private def actionTakenMessage(using Lang) = I18nKey.msg.modActionFeedback.txt()
