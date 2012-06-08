package lila
package bookmark

import core.CoreEnv

import game.DbGame
import user.User

trait BookmarkHelper {

  protected def env: CoreEnv
  private def api = env.bookmark.api

  def bookmarked(game: DbGame, user: User): Boolean = 
    api.bookmarked(game, user)
}
