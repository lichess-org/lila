package lila.round

import lila.common.Bus
import lila.game.Game
import lila.hub.actorApi.chat._
import lila.i18n.I18nKey.{ Select ⇒ SelectI18nKey }
import lila.i18n.I18nKeys

final class Messenger(
    bus: Bus,
    i18nKeys: I18nKeys) {

  def apply(game: Game, message: SelectI18nKey, args: Any*) {
    val translated = message(i18nKeys).en(args: _*)
    bus.publish(SystemTalk(game.id + "/w", translated), 'chatIn)
    if (game.nonAi) bus.publish(SystemTalk(game.id, translated), 'chatIn)
  }
}
