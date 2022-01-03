package shogi
package format.usi

case class UsiCharPair(a: Char, b: Char) {

  override def toString = s"$a$b"
}

object UsiCharPair {

  import implementation._

  def apply(usi: Usi): UsiCharPair = {
    usi match {
      case Usi.Move(orig, dest, false) => UsiCharPair(toChar(orig), toChar(dest))
      case Usi.Move(orig, dest, true)  => UsiCharPair(toChar(orig), toChar(dest, true))
      case Usi.Drop(role, pos) =>
        UsiCharPair(
          toChar(pos),
          dropRole2charMap.getOrElse(role, voidChar)
        )
    }
  }

  private[format] object implementation {

    type File = Int

    val charShift = 34        // Start at Char(34) == '"'
    val voidChar  = 33.toChar // '!'

    val pos2charMap: Map[Pos, Char] = Pos.all9x9
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
