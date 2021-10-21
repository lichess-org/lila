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
      conv(Move(SQ1G, SQ1F)) must_== "<E"
      conv(Move(SQ7C, SQ7D)) must_== "ZQ"
      conv(Move(SQ7G, SQ7F)) must_== "6?"
    }
    "unicity" in {
      allPairs.distinct.size must_== allMoves.size
    }
    "no void char" in {
      allPairs.count(_ contains UciCharPair.implementation.voidChar) must_== 0
    }
    "promotions" in {
      conv(Move(SQ8H, SQ2B, true)) must_== ",è"
      conv(Move(SQ8H, SQ2B, false)) must_== ",h"
    }
    "drops" in {
      conv(Drop(Pawn, SQ1G)) must_== "<ö"
      conv(Drop(Lance, SQ1A)) must_== "rù"
      conv(Drop(Silver, SQ1I)) must_== "*ø"
      conv(Drop(Bishop, SQ5E)) must_== "Jô"
    }
  }
}
