package lila.game

import lila.user.User

case class ListMenu(
  nbGames: Int,
  nbMates: Int,
  nbPopular: Int,
  nbBookmarks: Option[Int],
  nbAnalysed: Int,
  nbImported: Int)

object ListMenu {

  type CountBookmarks = User => Fu[Int]

  def apply(cached: Cached)(
    countBookmarks: CountBookmarks,
    countAnalysed: () => Fu[Int],
    me: Option[User]): Fu[ListMenu] =
    cached.nbGames zip
      cached.nbMates zip
      cached.nbPopular zip
      me.??(countBookmarks) zip
      countAnalysed() zip
      cached.nbImported map {
        case (((((nbGames, nbMates), nbPopular), nbBookmarks), nbAnalysed), nbImported) =>
          new ListMenu(
            nbGames = nbGames,
            nbMates = nbMates,
            nbPopular = nbPopular,
            nbBookmarks = nbBookmarks.some.filterNot(0 ==),
            nbAnalysed = nbAnalysed,
            nbImported = nbImported)
      }
}
