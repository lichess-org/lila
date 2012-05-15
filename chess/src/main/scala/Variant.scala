package lila.chess

import scala.util.Random
import scalaz.{ States }

import Pos.posAt

sealed abstract class Variant(val id: Int) {

  lazy val name = toString.toLowerCase

  def standard = this == Variant.Standard

  def exotic = !standard

  def pieces: Map[Pos, Piece]
}

object Variant {

  private def symmetricRank(rank: IndexedSeq[Role]): Map[Pos, Piece] =
      (for (y ← Seq(1, 2, 7, 8); x ← 1 to 8) yield {
        posAt(x, y) map { pos ⇒
          (pos, y match {
            case 1 ⇒ White - rank(x - 1)
            case 2 ⇒ White.pawn
            case 7 ⇒ Black.pawn
            case 8 ⇒ Black - rank(x - 1)
          })
        }
      }).flatten.toMap

  case object Standard extends Variant(1) {

    val pieces = symmetricRank(
      IndexedSeq(Rook, Knight, Bishop, Queen, King, Bishop, Knight, Rook)
    )
  }

  case object Chess960 extends Variant(2) with States {

    def pieces = symmetricRank {
      val size = 8
      type Rank = IndexedSeq[Option[Role]]
      def ?(max: Int) = Random nextInt max
      def empty(rank: Rank, skip: Int): Option[Int] = {
        1 to size find (x ⇒ (rank take x count (_.isEmpty)) == skip + 1)
      } map (_ - 1)
      def update(rank: Rank, role: Role)(x: Int): Rank =
        rank.updated(x, role.some)
      def place(rank: Rank, role: Role, x: Int): Option[Rank] = 
        empty(rank, x) map update(rank, role)
      val bishops: Rank =
        IndexedSeq.fill(8)(none[Role])
          .updated(2 * ?(4), Bishop.some) // place first bishop
          .updated(2 * ?(4) + 1, Bishop.some) // place second bishop

      val rank = for {
        a1 ← bishops.some
        a2 ← place(a1, Queen, ?(6))
        a3 ← place(a2, Knight, ?(5)) 
        a4 ← place(a3, Knight, ?(4)) 
        a5 ← place(a4, Rook, 0) 
        a6 ← place(a5, King, 0) 
        a7 ← place(a6, Rook, 0) 
      } yield a7

      rank.err("WTF").flatten
    }
  }

  val all = List(Standard, Chess960)

  val byId = all map { v ⇒ (v.id, v) } toMap

  val default = Standard

  def apply(id: Int): Option[Variant] = byId get id

  def orDefault(id: Int): Variant = apply(id) | default

  def exists(id: Int): Boolean = byId contains id
}
