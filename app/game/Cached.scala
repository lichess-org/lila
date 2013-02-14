package lila
package game

import akka.util.duration._

import memo.ActorMemo

private[game] final class Cached(
    gameRepo: GameRepo,
    nbTtl: Int) {

  import Cached._

  def nbGames: Int = memo(NbGames)
  def nbMates: Int = memo(NbMates)
  def nbPopular: Int = memo(NbPopular)

  private val memo = ActorMemo(loadFromDb, nbTtl, 5.seconds)

  private def loadFromDb(key: Key) = key match {
    case NbGames   ⇒ gameRepo.count(_.all).unsafePerformIO
    case NbMates   ⇒ gameRepo.count(_.mate).unsafePerformIO
    case NbPopular ⇒ gameRepo.count(_.popular).unsafePerformIO
  }
}

private[game] object Cached {

  sealed trait Key

  case object NbGames extends Key
  case object NbMates extends Key
  case object NbPopular extends Key
}
