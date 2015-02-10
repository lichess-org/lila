package lila.mod

import lila.game.Game
import lila.user.User

final class BoostingApi(modApi: ModApi) {

  def check(game: Game, whiteUser: User, blackUser: User): Funit = {
    fuccess {
      println(s"Check game $game for boosting, white: $whiteUser, black: $blackUser")
    }
  }
}
