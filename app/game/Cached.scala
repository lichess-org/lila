package lila
package game

import memo.Builder

final class Cached(
    gameRepo: GameRepo,
    nbTtl: Int) {

  import Cached._

  private val nbCache = Builder.cache[Key, Int](nbTtl, {
    case NbGames ⇒ gameRepo.count(_.all).unsafePerformIO
    case NbMates ⇒ gameRepo.count(_.mate).unsafePerformIO
  })

  def nbGames: Int = nbCache get NbGames

  def nbMates: Int = nbCache get NbMates
}

object Cached {

  sealed trait Key

  case object NbGames extends Key
  case object NbMates extends Key
}
