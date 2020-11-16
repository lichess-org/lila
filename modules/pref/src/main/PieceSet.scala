package lila.pref

sealed class PieceSet private[pref] (val name: String) {

  override def toString = name

  def cssClass = name
}

sealed trait PieceSetObject {

  val all: List[PieceSet]

  val default: PieceSet

  lazy val allByName = all map { c =>
    c.name -> c
  } toMap

  def apply(name: String) = allByName.getOrElse(name, default)

  def contains(name: String) = allByName contains name
}

object PieceSet extends PieceSetObject {

  val default = new PieceSet("kanji_light")

  val all = List(
    default.name,
    "kanji_brown",
    "orangain",
    "international",
    "kanji_red_wood"
  ) map { name =>
    new PieceSet(name)
  }
}

object PieceSet3d extends PieceSetObject {

  val default = new PieceSet("Basic")

  val all = List(
    default.name
  ) map { name =>
    new PieceSet(name)
  }
}
