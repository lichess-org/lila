package lila.game

import lila.user.User

case class ListMenu(
  nbGames: Int,
  nbMates: Int,
  nbBookmarks: Option[Int],
  nbAnalysed: Int,
  nbImported: Int,
  nbRelayed: Int)

object ListMenu {

  def apply(cached: Cached)(
    countBookmarks: User => Int,
    countAnalysed: () => Fu[Int],
    me: Option[User]): Fu[ListMenu] =
    cached.nbGames zip
      cached.nbMates zip
      countAnalysed() zip
      cached.nbImported zip
      cached.nbRelayed map {
        case ((((nbGames, nbMates), nbAnalysed), nbImported), nbRelayed) =>
          new ListMenu(
            nbGames = nbGames,
            nbMates = nbMates,
            nbBookmarks = me.??(countBookmarks).some.filterNot(0 ==),
            nbAnalysed = nbAnalysed,
            nbImported = nbImported,
            nbRelayed = nbRelayed)
      }
}
