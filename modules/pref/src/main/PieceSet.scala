package lila.pref

// key is tied to file names and in db, title can be easily changed
sealed class PieceSet private[pref] (val key: String, val name: String) {

  override def toString = key

  def cssClass = key

}

sealed trait PieceSetBase {

  val default: PieceSet

  val all: List[PieceSet]

  lazy val allByKey = all map { p =>
    p.key -> p
  } toMap

  def apply(key: String) = allByKey.getOrElse(key, default)

  def contains(key: String) = allByKey contains key

}

object PieceSet extends PieceSetBase {

  val default = new PieceSet("Ryoko_1Kanji", "Ryoko 1-Kanji")

  val all = List(
    default,
    new PieceSet("kanji_light", "Kanji Light"),
    new PieceSet("kanji_brown", "Kanji Brown"),
    new PieceSet("orangain", "orangain"),
    new PieceSet("kanji_red_wood", "Kanji Red Wood"),
    new PieceSet("Portella", "Portella"),
    new PieceSet("Portella_2Kanji", "Portella 2-Kanji"),
    new PieceSet("1Kanji_3D", "Kanji 3D"),
    new PieceSet("2Kanji_3D", "2-Kanji 3D"),
    new PieceSet("Shogi_cz", "Shogi.cz"),
    new PieceSet("Shogi_FCZ", "Czech"),
    new PieceSet("Engraved_cz", "Engraved Shogi.cz"),
    new PieceSet("Engraved_cz_BnW", "Engraved Shogi.cz - black and white"),
    new PieceSet("Kanji_Guide_Shadowed", "Kanji Guide Shadowed"),
    new PieceSet("Valdivia", "Valdivia"),
    new PieceSet("Vald_opt", "Valdivia 2"),
    new PieceSet("shogi_BnW", "Shogi - black and white"),
    new PieceSet("pixel", "Pixel 8bit"),
    new PieceSet("Intl_Colored_2D", "International Colored 2D"),
    new PieceSet("Intl_Colored_3D", "International Colored 3D"),
    new PieceSet("Intl_Shadowed", "International Shadowed"),
    new PieceSet("Intl_Monochrome_2D", "International Monochrome 2D"),
    new PieceSet("Intl_Wooden_3D", "International Wooden 3D"),
    new PieceSet("Intl_Portella", "International Portella"),
    new PieceSet("international", "International"),
    new PieceSet("joyful", "Joyful"),
    new PieceSet("simple_kanji", "Simple Kanji"),
    new PieceSet("doubutsu", "Doubutsu"),
    new PieceSet("Logy_Games", "Logy Games"),
    new PieceSet("western", "Western")
  )

}

object ChuPieceSet extends PieceSetBase {

  val default = new PieceSet("Chu_Ryoko_1Kanji", "Ryoko 1-Kanji")

  val all = List(
    default,
    new PieceSet("Chu_Eigetsu_Gyoryu", "Eigetsu Gyoryu"),
    new PieceSet("Chu_Intl", "International"),
    new PieceSet("Chu_FCZ", "Czech"),
    new PieceSet("Chu_Intl_BnW", "International - black and white"),
    new PieceSet("Chu_Mnemonic", "Mnemonic")
  )

}

object KyoPieceSet extends PieceSetBase {

  val default = new PieceSet("Kyo_Ryoko_1Kanji", "Ryoko 1-Kanji")

  val all = List(
    default,
    new PieceSet("Kyo_orangain", "orangain"),
    new PieceSet("Kyo_Kanji", "Kanji with promotions"),
    new PieceSet("Kyo_Intl", "International with promotions"),
    new PieceSet("Kyo_international", "International"),
    new PieceSet("Kyo_simple_kanji", "Simple Kanji"),
    new PieceSet("Kyo_doubutsu", "Doubutsu"),
    new PieceSet("Kyo_joyful", "Joyful"),
    new PieceSet("Kyo_Logy_Games", "Logy Games")
  )

}
