package lila.round

import lila.game.Game
import lila.pref.{ Pref, PrefApi }

private[round] final class MoreTimeAble(prefApi: PrefApi) {
  def isAllowedByPrefs(game: Game): Fu[Boolean] =
    game.userIds.map { userId =>
      prefApi.getPref(userId, (p: Pref) => p.moretimeable)
    }.sequenceFu map {
      _.forall { p =>
        p == Pref.MoreTimeAble.ALWAYS || (p == Pref.MoreTimeAble.CASUAL && game.casual)
      }
    }
}
