package lila.game

import ornicar.scalalib.Zero
import scala.util.{ Try, Success, Failure }

sealed trait Blurs extends Any {

  def nb: Int

  def isEmpty = nb == 0

  // Blurs.Nb is deprecated and read only;
  // any write will result in a Blurs.Bits
  def add(moveIndex: Int): Blurs.Bits
}

object Blurs {

  case class Nb(nb: Int) extends AnyVal with Blurs {

    def add(moveIndex: Int) = blursZero.zero add moveIndex

    override def toString = s"Blurs.Nb($nb)"
  }

  case class Bits(bits: Long) extends AnyVal with Blurs {

    def nb = java.lang.Long.bitCount(bits)

    def add(moveIndex: Int) =
      if (moveIndex < 0 || moveIndex > 63) this
      else Bits(bits | (1L << moveIndex))

    def asInt = ((bits >>> 32) == 0) option bits.toInt

    def binaryString = java.lang.Long.toBinaryString(bits).reverse

    def booleans = binaryString.toArray.map('1'==)

    override def toString = s"Blurs.Bits($binaryString)"
  }

  implicit val blursZero = Zero.instance[Blurs](Bits(0L))

  import reactivemongo.api.bson._

  private[game] implicit val BlursBitsBSONHandler = lila.db.BSON.tryHandler[Bits](
    {
      case BSONInteger(bits) => Success(Bits(bits & 0xffffffffL))
      case BSONLong(bits) => Success(Bits(bits))
      case v => lila.db.BSON.handlerBadValue(s"Invalid blurs bits $v")
    },
    bits => bits.asInt.fold[BSONValue](BSONLong(bits.bits))(BSONInteger.apply)
  )

  private[game] implicit val BlursNbBSONHandler = BSONIntegerHandler.as[Nb](Nb.apply, _.nb)

  private[game] implicit val BlursBSONWriter = new BSONWriter[Blurs] {
    def writeTry(b: Blurs) = b match {
      case bits: Bits => BlursBitsBSONHandler writeTry bits
      // only Bits can be written; Nb results to Bits(0)
      case _ => Success[BSONValue](BSONInteger(0))
    }
  }
}
