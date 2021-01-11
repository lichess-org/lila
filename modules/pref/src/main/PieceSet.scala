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

  val default = new PieceSet("Ryoko_1Kanji")

  val all = List(
    "kanji_light",
    "kanji_brown",
    default.name,
    "orangain",
    "kanji_red_wood",
    "1Kanji_3D",
    "2Kanji_3D",
    "Shogi_cz",
    "Kanji_Guide_Shadowed",
    "Valdivia",
    "Vald_opt",
    "Intl_Colored_2D",
    "Intl_Colored_3D",
    "Intl_Colored_Shadowed",
    "Intl_Monochrome_2D",
    "Intl_Wooden_3D",
    "international",
    "simple_kanji",
    "doubutsu",
    "Logy_Games"
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
