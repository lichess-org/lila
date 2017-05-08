package lila.game

import ornicar.scalalib.Zero
import scala.collection.immutable.BitSet

sealed trait Blurs extends Any {

  def nb: Int

  def bitsOption: Option[Long]
  def bitSetOption: Option[BitSet]

  def isEmpty = nb == 0

  // Blurs.Nb is deprecated and read only;
  // any write will result in a Blurs.Bits
  def add(moveIndex: Int): Blurs.Bits
}

object Blurs {

  case class Nb(nb: Int) extends AnyVal with Blurs {

    def bitsOption = none
    def bitSetOption = none

    def add(moveIndex: Int) = blursZero.zero add moveIndex
  }

  case class Bits(bits: Long) extends AnyVal with Blurs {

    def bitSet = new BitSet.BitSet1(bits)

    def nb = java.lang.Long.bitCount(bits)

    def bitsOption = bits.some
    def bitSetOption = bitSet.some

    def add(moveIndex: Int) =
      if (moveIndex < 0 || moveIndex > 63) this
      else Bits(bits | (1L << moveIndex))

    def asInt = (bits <= Int.MaxValue) option bits.toInt

    override def toString = java.lang.Long.toBinaryString(bits).reverse
  }

  implicit val blursZero = Zero.instance[Blurs](Bits(0l))

  import reactivemongo.bson._

  private[game] implicit val BlursBitsBSONHandler = new BSONHandler[BSONValue, Bits] {
    def read(bv: BSONValue): Bits = bv match {
      case BSONInteger(bits) => Bits(bits)
      case BSONLong(bits) => Bits(bits)
      case v => sys error s"Invalid blurs bits $v"
    }
    def write(b: Bits): BSONValue =
      b.asInt.fold[BSONValue](BSONLong(b.bits))(BSONInteger.apply)
  }

  private[game] implicit val BlursNbBSONReader = new BSONReader[BSONInteger, Nb] {
    def read(bi: BSONInteger) = Nb(bi.value)
  }

  private[game] implicit val BlursBSONWriter = new BSONWriter[Blurs, BSONValue] {
    def write(b: Blurs): BSONValue = b match {
      case bits: Bits => BlursBitsBSONHandler write bits
      // only Bits can be written; Nb results to Bits(0)
      case _ => BSONInteger(0)
    }
  }
}
