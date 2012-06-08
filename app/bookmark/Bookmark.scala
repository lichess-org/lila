package lila
package bookmark

import game.DbGame
import user.User

import org.joda.time.DateTime

case class Bookmark(
  game: DbGame,
  user: User,
  date: DateTime) {

}
