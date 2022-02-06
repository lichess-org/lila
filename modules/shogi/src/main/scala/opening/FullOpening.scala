package shogi
package opening

final class FullOpening(
    val japanese: String,
    val english: String,
    val sfen: String
) {

  def ecoName = s"$japanese ($english)"

  override def toString = ecoName

  def atPly(ply: Int) = FullOpening.AtPly(this, ply)
}

object FullOpening {

  case class AtPly(opening: FullOpening, ply: Int)
}
