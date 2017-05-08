package lila.game

import ornicar.scalalib.Zero
import scala.collection.immutable.BitSet

sealed trait Blurs extends Any {

  def nb: Int

  def bitsOption: Option[BitSet]

  def isEmpty = nb == 0

  // Blurs.Nb is deprecated and read only;
  // any write will result in a Blurs.Bits
  def add(moveIndex: Int): Blurs.Bits
}

object Blurs {

  case class Nb(nb: Int) extends AnyVal with Blurs {

    def bitsOption = none

    def add(moveIndex: Int) = blursZero.zero add moveIndex
  }

  case class Bits(bits: Long) extends AnyVal with Blurs {

    def bitSet = new BitSet.BitSet1(bits)

    def nb = java.lang.Long.bitCount(bits)

    def bitsOption = bitSet.some

    def add(moveIndex: Int) =
      if (moveIndex < 0 || moveIndex > 63) this
      else Bits(bits | (1L << moveIndex))
  }

  implicit val blursZero = Zero.instance[Blurs](Bits(0l))

  import reactivemongo.bson._

  implicit val BlursBSONHandler = new BSONHandler[BSONValue, Blurs] {
    def read(bv: BSONValue): Blurs = bv match {
      case BSONInteger(nb) => Nb(nb)
      case BSONLong(bits) => Bits(bits)
      case v => sys error s"Invalid blurs $v"
    }
    def write(b: Blurs) = b match {
      case Nb(nb) => BSONInteger(nb)
      case Bits(bits) => BSONLong(bits)
    }
  }
}
