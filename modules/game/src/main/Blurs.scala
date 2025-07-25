package lila.game

import alleycats.Zero

import scala.util.Success

import lila.core.game.Blurs
import lila.core.game.Blurs as apply

object Blurs:

  import reactivemongo.api.bson.*
  private[game] given blursHandler: BSONHandler[Blurs] = lila.db.dsl.tryHandler[Blurs](
    {
      case BSONInteger(bits) => Success(apply(bits & 0xffffffffL))
      case BSONLong(bits) => Success(apply(bits))
      case v => lila.db.BSON.handlerBadValue(s"Invalid blurs bits $v")
    },
    blurs => blurs.asInt.fold[BSONValue](BSONLong(blurs.value))(BSONInteger.apply)
  )

  extension (bits: Blurs)

    def addAtMoveIndex(moveIndex: Int): Blurs =
      if moveIndex < 0 || moveIndex > 63 then bits
      else apply(bits.value | (1L << moveIndex))

    def asInt = ((bits.value >>> 32) == 0).option(bits.value.toInt)

    def binaryString: String = java.lang.Long.toBinaryString(bits.value).reverse

    def booleans: Array[Boolean] = binaryString.toArray.map('1' ==)

    def nonEmpty = bits.value != 0

  given zeroBlurs: Zero[Blurs] = Zero(apply(0L))
