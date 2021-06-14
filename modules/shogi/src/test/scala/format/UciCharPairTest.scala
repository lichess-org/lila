package shogi
package format

import Pos._
import Uci._

class UciCharPairTest extends ShogiTest {

  // println(UciCharPair.implementation.pos2charMap.toList.sortBy(_._2.toInt))
  // println(UciCharPair.implementation.promotion2charMap.toList.sortBy(_._2.toInt))
  // println(UciCharPair.implementation.dropRole2charMap.toList.sortBy(_._2.toInt).map(x => x._1 -> x._2.toInt))

  "char pair encoding" should {

    def conv(uci: Uci) = UciCharPair(uci).toString

    val allMoves = for {
      orig <- Pos.all
      dest <- Pos.all
    } yield Move(orig, dest)
    val allPairs = allMoves.map(conv)

    "regular moves" in {
      conv(Move(I3, I4)) must_== "<E"
      conv(Move(C7, C6)) must_== "ZQ"
      conv(Move(C3, C4)) must_== "6?"
    }
    "unicity" in {
      allPairs.distinct.size must_== allMoves.size
    }
    "no void char" in {
      allPairs.count(_ contains UciCharPair.implementation.voidChar) must_== 0
    }
    "promotions" in {
      conv(Move(B2, H8, true)) must_== ",è"
      conv(Move(B2, H8, false)) must_== ",h"
    }
    "drops" in {
      conv(Drop(Pawn, I3)) must_== "<ö"
      conv(Drop(Lance, I9)) must_== "rù"
      conv(Drop(Silver, I1)) must_== "*ø"
      conv(Drop(Bishop, E5)) must_== "Jô"
    }
  }
}
