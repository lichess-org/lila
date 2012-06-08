package lila
package game

import user.User

import scalaz.effects._

case class ListMenu(
  nbGames: Int,
  nbMates: Int,
  nbStars: Option[Int])

object ListMenu {

  type CountStars = User ⇒ IO[Int]

  def apply(cached: Cached)(countStars: CountStars, me: Option[User]): IO[ListMenu] =
    me.fold(
      m ⇒ countStars(m) map (_.some),
      io(none)
    ) map { nbStars ⇒
        new ListMenu(
          nbGames = cached.nbGames,
          nbMates = cached.nbMates,
          nbStars = nbStars)
      }
}
