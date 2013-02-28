package lila.app
package analyse

import scala.concurrent.duration._

import lila.common.memo.ActorMemo

final class Cached(
    analysisRepo: AnalysisRepo,
    nbTtl: Int) {

  import Cached._

  def nbAnalysis: Int = memo(NbAnalysis)

  private val memo = ActorMemo(loadFromDb, nbTtl, 5.seconds)

  private def loadFromDb(key: Key) = key match {
    case NbAnalysis ⇒ analysisRepo.collection.count.toInt
  }
}

object Cached {

  sealed trait Key

  case object NbAnalysis extends Key
}
