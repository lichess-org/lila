package lila
package star

import core.CoreEnv

import game.DbGame
import user.User

trait StarHelper {

  protected def env: CoreEnv
  private def api = env.star.api

  def starred(game: DbGame, user: User): Boolean = 
    api.starred(game, user).unsafePerformIO
}
