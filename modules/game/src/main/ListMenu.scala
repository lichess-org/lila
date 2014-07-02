package lila.game

import lila.user.User

case class ListMenu(
  nbGames: Int,
  nbMates: Int,
  nbBookmarks: Option[Int],
  nbAnalysed: Int,
  nbImported: Int)

object ListMenu {

  def apply(cached: Cached)(
    countBookmarks: User => Int,
    countAnalysed: () => Fu[Int],
    me: Option[User]): Fu[ListMenu] =
    cached.nbGames zip
      cached.nbMates zip
      countAnalysed() zip
      cached.nbImported map {
        case (((nbGames, nbMates), nbAnalysed), nbImported) =>
          new ListMenu(
            nbGames = nbGames,
            nbMates = nbMates,
            nbBookmarks = me.??(countBookmarks).some.filterNot(0 ==),
            nbAnalysed = nbAnalysed,
            nbImported = nbImported)
      }
}
