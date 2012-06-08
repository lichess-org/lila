package lila
package game

import user.User

import scalaz.effects._

case class ListMenu(
  nbGames: Int,
  nbMates: Int,
  nbPopular: Int,
  nbBookmarks: Option[Int])

object ListMenu {

  type CountBookmarks = User ⇒ Int

  def apply(cached: Cached)(countBookmarks: CountBookmarks, me: Option[User]): ListMenu =
    new ListMenu(
      nbGames = cached.nbGames,
      nbMates = cached.nbMates,
      nbPopular = cached.nbPopular,
      nbBookmarks = me map countBookmarks)
}
