package shogi
package format.usi

import shogi.variant.Variant

// We are trying to map every possible legal usi in a given situation to a unique char pair
case class UsiCharPair(a: Char, b: Char) {
  override def toString = s"$a$b"
}

// While char is 16 bit
// we don't want to go above one byte
// and 251-255 reserved for some special cases
object UsiCharPair {

  def apply(usi: Usi, variant: Variant): UsiCharPair =
    usi match {
      case Usi.Move(orig, dest, false) => 
        UsiCharPair(posToChar(variant, orig), posToChar(variant, dest))
      // If we are moving from orig to dest, we know it's not possible to move from dest to orig
      // Therefore that combination can be used for promotions
      case Usi.Move(orig, dest, true) =>
        UsiCharPair(posToChar(variant, dest), posToChar(variant, orig))
      case Usi.Drop(role, pos) =>
        UsiCharPair(
          posToChar(variant, pos),
          roleToChar(variant, role)
        )
    }

    val charOffset = 35        // Start at Char(35) == '#'
    val voidChar   = 33.toChar // '!'

    def posToChar(variant: Variant, pos: Pos): Char =
      (charOffset + pos.rank.index * variant.numberOfFiles + pos.file.index).toChar

    def roleToChar(variant: Variant, role: Role): Char =
      variant.handRoles.zipWithIndex
        .find(_._1 == role)
        .map { case (_, i) => (charOffset + variant.allPositions.size + i).toChar }
        .getOrElse(voidChar)

}
