package chess
package format

import Pos._
import Uci._

class UciCharPairTest extends ChessTest {

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
      conv(Move(A1, B1)) must_== "#$"
      conv(Move(A1, A2)) must_== "#+"
      conv(Move(H7, H8)) must_== "Zb"
    }
    "unicity" in {
      allPairs.distinct.size must_== allMoves.size
    }
    "no void char" in {
      allPairs.count(_ contains UciCharPair.implementation.voidChar) must_== 0
    }
    "promotions" in {
      conv(Move(B7, B8, Some(Queen))) must_== "Td"
      conv(Move(B7, C8, Some(Queen))) must_== "Te"
      conv(Move(B7, C8, Some(Knight))) must_== "T}"
    }
    "drops" in {
      conv(Drop(Pawn, A1)).head must_== '#'
      conv(Drop(Pawn, A1)).tail.head.toInt must_== 143
      conv(Drop(Queen, H8)).head must_== 'b'
      conv(Drop(Queen, H8)).tail.head.toInt must_== 139
    }
  }
}
