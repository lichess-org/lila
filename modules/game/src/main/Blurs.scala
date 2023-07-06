package lila.game

import alleycats.Zero
import scala.util.Success

opaque type Blurs = Long
object Blurs extends OpaqueLong[Blurs]:

  given lila.db.NoDbHandler[Blurs] with {}

  import reactivemongo.api.bson.*
  private[game] given blursHandler: BSONHandler[Blurs] = lila.db.dsl.tryHandler[Blurs](
    {
      case BSONInteger(bits) => Success(Blurs(bits & 0xffffffffL))
      case BSONLong(bits)    => Success(Blurs(bits))
      case v                 => lila.db.BSON.handlerBadValue(s"Invalid blurs bits $v")
    },
    blurs => blurs.asInt.fold[BSONValue](BSONLong(blurs.value))(BSONInteger.apply)
  )

  extension (bits: Blurs)

    def nb = java.lang.Long.bitCount(bits)

    def add(moveIndex: Int): Blurs =
      if moveIndex < 0 || moveIndex > 63 then bits
      else Blurs(bits | (1L << moveIndex))

    def asInt = ((bits >>> 32) == 0) option bits.toInt

    def binaryString: String = java.lang.Long.toBinaryString(bits).reverse

    def booleans: Array[Boolean] = binaryString.toArray.map('1' ==)

    def nonEmpty = bits != 0

  given zeroBlurs: Zero[Blurs] = Zero(Blurs(0L))
