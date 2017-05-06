package lila.pref

sealed class PieceSet private[pref] (val name: String) {

  override def toString = name

  def cssClass = name
}

sealed trait PieceSetObject {

  val all: List[PieceSet]

  val allByName = all map { c => c.name -> c } toMap

  val default = all.head

  def apply(name: String) = allByName.getOrElse(name, default)

  def contains(name: String) = allByName contains name
}

object PieceSet extends PieceSetObject {

  val all = List(
    "cburnett", "merida", "alpha", "pirouetti",
    "chessnut", "chess7", "reillycraig", "companion",
    "fantasy", "spatial", "shapes", "letter"
  ) map { name => new PieceSet(name) }
}

object PieceSet3d extends PieceSetObject {

  val all = List(
    "Basic", "Wood", "Metal", "RedVBlue",
    "ModernJade", "ModernWood", "Glass", "Trimmed",
    "Experimental", "Staunton"
  ) map { name => new PieceSet(name) }
}
