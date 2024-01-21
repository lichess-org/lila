package lila.pref

sealed class PieceSet private[pref] (val name: String) {

  override def toString = name

  def cssClass = name
}

sealed trait PieceSetBase {

  val default: PieceSet

  val all: List[PieceSet]

  lazy val allByName = all map { c =>
    c.name -> c
  } toMap

  def apply(name: String) = allByName.getOrElse(name, default)

  def contains(name: String) = allByName contains name

}

object PieceSet extends PieceSetBase {

  val default = new PieceSet("Ryoko_1Kanji")

  val all = List[String](
    "kanji_light",
    "kanji_brown",
    default.name,
    "orangain",
    "kanji_red_wood",
    "Portella",
    "Portella_2Kanji",
    "1Kanji_3D",
    "2Kanji_3D",
    "Shogi_cz",
    "Engraved_cz",
    "Kanji_Guide_Shadowed",
    "Valdivia",
    "Vald_opt",
    "shogi_BnW",
    "pixel",
    "Intl_Colored_2D",
    "Intl_Colored_3D",
    "Intl_Shadowed",
    "Intl_Monochrome_2D",
    "Intl_Wooden_3D",
    "Intl_Portella",
    "international",
    "joyful",
    "simple_kanji",
    "doubutsu",
    "Logy_Games",
    "western"
  ) map { name =>
    new PieceSet(name)
  }

}

object ChuPieceSet extends PieceSetBase {

  val default = new PieceSet("Chu_Ryoko_1Kanji")

  val all = List[String](
    default.name,
    "Chu_Intl",
    "Chu_Intl_BnW",
    "Chu_Mnemonic"
  ) map { name =>
    new PieceSet(name)
  }

}

object KyoPieceSet extends PieceSetBase {

  val default = new PieceSet("Kyo_Ryoko_1Kanji")

  val all = List[String](
    default.name,
    "Kyo_orangain",
    "Kyo_Kanji",
    "Kyo_Intl",
    "Kyo_international",
    "Kyo_simple_kanji",
    "Kyo_doubutsu",
    "Kyo_Logy_Games"
  ) map { name =>
    new PieceSet(name)
  }

}
