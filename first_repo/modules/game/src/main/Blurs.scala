package lila.game

import ornicar.scalalib.Zero
import scala.util.Success

case class Blurs(bits: Long) extends AnyVal {

  def nb = java.lang.Long.bitCount(bits)

  def add(moveIndex: Int) =
    if (moveIndex < 0 || moveIndex > 63) this
    else Blurs(bits | (1L << moveIndex))

  def asInt = ((bits >>> 32) == 0) option bits.toInt

  def binaryString = java.lang.Long.toBinaryString(bits).reverse

  def booleans = binaryString.toArray.map('1' ==)

  def nonEmpty = bits != 0

  override def toString = s"Blurs.Bits($binaryString)"
}

object Blurs {

  implicit val blursZero = Zero.instance(Blurs(0L))

  import reactivemongo.api.bson._

  implicit private[game] val BlursBSONHandler = lila.db.dsl.tryHandler[Blurs](
    {
      case BSONInteger(bits) => Success(Blurs(bits & 0xffffffffL))
      case BSONLong(bits)    => Success(Blurs(bits))
      case v                 => lila.db.BSON.handlerBadValue(s"Invalid blurs bits $v")
    },
    blurs => blurs.asInt.fold[BSONValue](BSONLong(blurs.bits))(BSONInteger.apply)
  )
}
