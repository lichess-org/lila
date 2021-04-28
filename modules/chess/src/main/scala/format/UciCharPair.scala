package chess
package format

case class UciCharPair(a: Char, b: Char) {

  override def toString = s"$a$b"
}

object UciCharPair {

  import implementation._

  def apply(uci: Uci): UciCharPair = {
    uci match {
      case Uci.Move(orig, dest, false) => UciCharPair(toChar(orig), toChar(dest))
      case Uci.Move(orig, dest, true)  => UciCharPair(toChar(orig), toChar(dest, true))
      case Uci.Drop(role, pos) =>
        UciCharPair(
          toChar(pos),
          dropRole2charMap.getOrElse(role, voidChar)
        )
    }
  }

  private[format] object implementation {

    type File = Int

    val charShift = 34        // Start at Char(34) == '"'
    val voidChar  = 33.toChar // '!'

    val pos2charMap: Map[Pos, Char] = Pos.all
      .map { pos =>
        pos -> (pos.hashCode + charShift).toChar
      }
      .to(Map)

    def toChar(pos: Pos) = pos2charMap.getOrElse(pos, voidChar)

    def toChar(pos: Pos, prom: Boolean): Char = {
      if (prom) (toChar(pos) + 128).toChar else toChar(pos)
    }

    val dropRole2charMap: Map[Role, Char] =
      Role.all
        .filterNot(r =>
          r == King ||
            r == Tokin ||
            r == PromotedLance ||
            r == PromotedKnight ||
            r == PromotedSilver ||
            r == Horse ||
            r == Dragon
        ) // todo nicer - res size -> 7
        .zipWithIndex
        .map { case (role, index) =>
          role -> (charShift + pos2charMap.size + 128 + index).toChar
        }
        .to(Map)
  }
}
