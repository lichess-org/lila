package lila.playban

import lila.core.i18n.I18nKey.site as trans

final private class PlaybanFeedback(
    chatApi: lila.core.chat.ChatApi,
    lightUser: lila.core.LightUser.Getter
)(using Executor):

  def abort(pov: Pov): Unit = tell(pov, s"i18n.${trans.playbanFeedbackAbort} {user}")

  def noStart(pov: Pov): Unit = tell(pov, s"i18n.${trans.playbanFeedbackNoStart} {user}")

  def rageQuit(pov: Pov): Unit = tell(pov, s"i18n.${trans.playbanFeedbackRageQuit} {user}")

  def sitting(pov: Pov): Unit =
    tell(
      pov,
      s"i18n.${trans.playbanFeedbackSitting} {user}"
    )

  def quickResign(pov: Pov): Unit = tell(pov, s"i18n.${trans.playbanFeedbackQuickResign} {user}")

  private def tell(pov: Pov, template: String): Unit =
    pov.player.userId.foreach { userId =>
      lightUser(userId).foreach { light =>
        val message = template.replace("{user}", light.fold(userId.value)(_.name.value))
        chatApi.volatile(pov.gameId.into(ChatId), message, _.round)
      }
    }

object PlaybanFeedback:

  val sittingAutoPreset = lila.core.msg.MsgPreset(
    name = "Warning: leaving games / stalling on time",
    text =
      """In your game history, you have several games where you have left the game or just let the time run out instead of playing or resigning.
  This can be very annoying for your opponents. If this behavior continues to happen, we may be forced to terminate your account."""
  )
