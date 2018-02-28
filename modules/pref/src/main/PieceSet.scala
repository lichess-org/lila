package lila.pref

sealed class PieceSet private[pref] (val name: String) {

  override def toString = name

  def cssClass = name
}

sealed trait PieceSetObject {

  val all: List[PieceSet]

  val default: PieceSet

  lazy val allByName = all map { c => c.name -> c } toMap

  def apply(name: String) = allByName.getOrElse(name, default)

  def contains(name: String) = allByName contains name
}

object PieceSet extends PieceSetObject {

  val default = new PieceSet("cburnett")

  val all = List(
    default.name, "merida", "alpha", "pirouetti",
    "chessnut", "chess7", "reillycraig", "companion",
    "fantasy", "go", "spatial", "riohacha", "shapes", "letter"
  ) map { name => new PieceSet(name) }
}

object PieceSet3d extends PieceSetObject {

  val default = new PieceSet("Basic")

  val all = List(
    default.name, "Wood", "Metal", "RedVBlue",
    "ModernJade", "ModernWood", "Glass", "Trimmed",
    "Experimental", "Staunton"
  ) map { name => new PieceSet(name) }
}
