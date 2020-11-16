package chess
package opening

final class FullOpening(
    val eco: String,
    val name: String,
    val fen: String
) {

  def ecoName = s"$eco $name"

  override def toString = ecoName

  def atPly(ply: Int) = FullOpening.AtPly(this, ply)
}

object FullOpening {

  case class AtPly(opening: FullOpening, ply: Int)
}
