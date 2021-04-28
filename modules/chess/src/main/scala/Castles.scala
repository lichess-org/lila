package chess

final case class Castles(
    senteKingSide: Boolean,
    senteQueenSide: Boolean,
    goteKingSide: Boolean,
    goteQueenSide: Boolean
) {

  def can(color: Color) = new Castles.Can(this, color)

  def without(color: Color) =
    color match {
      case Sente =>
        copy(
          senteKingSide = false,
          senteQueenSide = false
        )
      case Gote =>
        copy(
          goteKingSide = false,
          goteQueenSide = false
        )
    }

  def without(color: Color, side: Side) =
    (color, side) match {
      case (Sente, KingSide)  => copy(senteKingSide = false)
      case (Sente, QueenSide) => copy(senteQueenSide = false)
      case (Gote, KingSide)   => copy(goteKingSide = false)
      case (Gote, QueenSide)  => copy(goteQueenSide = false)
    }

  def add(color: Color, side: Side) =
    (color, side) match {
      case (Sente, KingSide)  => copy(senteKingSide = true)
      case (Sente, QueenSide) => copy(senteQueenSide = true)
      case (Gote, KingSide)   => copy(goteKingSide = true)
      case (Gote, QueenSide)  => copy(goteQueenSide = true)
    }

  override lazy val toString: String = {
    (if (senteKingSide) "K" else "") +
      (if (senteQueenSide) "Q" else "") +
      (if (goteKingSide) "k" else "") +
      (if (goteQueenSide) "q" else "")
  } match {
    case "" => "-"
    case n  => n
  }

  def toSeq = Array(senteKingSide, senteQueenSide, goteKingSide, goteQueenSide)

  def isEmpty = !(senteKingSide || senteQueenSide || goteKingSide || goteQueenSide)
}

object Castles {

  def apply(
      castles: (Boolean, Boolean, Boolean, Boolean)
  ): Castles =
    new Castles(
      senteKingSide = castles._1,
      senteQueenSide = castles._2,
      goteKingSide = castles._3,
      goteQueenSide = castles._4
    )

  def apply(str: String): Castles =
    new Castles(
      str contains 'K',
      str contains 'Q',
      str contains 'k',
      str contains 'q'
    )

  val all  = new Castles(true, true, true, true)
  val none = new Castles(false, false, false, false)
  def init = all

  final class Can(castles: Castles, color: Color) {
    def on(side: Side): Boolean =
      (color, side) match {
        case (Sente, KingSide)  => castles.senteKingSide
        case (Sente, QueenSide) => castles.senteQueenSide
        case (Gote, KingSide)   => castles.goteKingSide
        case (Gote, QueenSide)  => castles.goteQueenSide
      }
    def any = on(KingSide) || on(QueenSide)
  }
}
