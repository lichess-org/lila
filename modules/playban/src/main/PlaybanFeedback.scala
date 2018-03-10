package lila.playban

import lila.chat.{ Chat, ChatApi }
import lila.game.Pov

private final class PlaybanFeedback(
    chatApi: ChatApi,
    lightUser: lila.common.LightUser.Getter
) {

  private val tempBan = "will result in a temporary ban."

  def abort(pov: Pov): Unit = tell(pov, s"Warning, {user}: aborting too many games $tempBan")

  def noStart(pov: Pov): Unit = tell(pov, s"Warning, {user}: failing to start games $tempBan")

  def rageQuit(pov: Pov): Unit = tell(pov, s"Warning, {user}: leaving games without resigning $tempBan")

  def sitting(pov: Pov): Unit = tell(pov, s"Warning, {user}: letting time run out instead of resigning $tempBan")

  def sandbag(pov: Pov): Unit = tell(pov, s"Warning, {user}: losing games on purpose will result in a ban.")

  private def tell(pov: Pov, template: String): Unit =
    pov.player.userId foreach { userId =>
      lightUser(userId) foreach { light =>
        val message = template.replace("{user}", light.fold(userId)(_.name))
        chatApi.userChat.volatile(Chat.Id(pov.gameId), message)
      }
    }
}
