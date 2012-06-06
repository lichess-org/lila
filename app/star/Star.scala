package lila
package star

import game.DbGame
import user.User

import org.joda.time.DateTime

case class Star(
  game: DbGame,
  user: User,
  date: DateTime) {

}
