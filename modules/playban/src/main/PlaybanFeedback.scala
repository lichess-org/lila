package lila.playban

import lila.chat.ChatApi
import lila.game.Pov

final private class PlaybanFeedback(
    chatApi: ChatApi,
    lightUser: lila.common.LightUser.Getter
)(using Executor):

  private val tempBan = "will result in a temporary ban."

  def abort(pov: Pov): Unit = tell(pov, s"Warning, {user}. Aborting too many games $tempBan")

  def noStart(pov: Pov): Unit = tell(pov, s"Warning, {user}. Failing to start games $tempBan")

  def rageQuit(pov: Pov): Unit = tell(pov, s"Warning, {user}. Leaving games without resigning $tempBan")

  def sitting(pov: Pov): Unit =
    tell(pov, s"Warning, {user}. Letting time run out instead of resigning $tempBan")

  private def tell(pov: Pov, template: String): Unit =
    pov.player.userId foreach { userId =>
      lightUser(userId) foreach { light =>
        val message = template.replace("{user}", light.fold(userId.value)(_.name.value))
        chatApi.userChat.volatile(pov.gameId into ChatId, message, _.Round)
      }
    }
