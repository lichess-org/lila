package lila.app
package templating

import lila.bookmark.Env.{ current => bookmarkEnv }

import lila.game.Game
import lila.user.User

trait BookmarkHelper {

  def bookmarked(game: Game, user: User): Boolean = 
    bookmarkEnv.api.bookmarked(game, user).await
}
