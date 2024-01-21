package lila.pref

sealed class Theme private[pref] (val name: String, val file: Option[String]) {

  override def toString = name

  def cssClass = name
}

object Theme {

  val default = new Theme("wood", "wood.png".some)

  val all = List(
    new Theme("orange", None),
    new Theme("natural", None),
    default,
    new Theme("wood1", "wood1.jpg".some),
    new Theme("kaya1", "kaya1.jpg".some),
    new Theme("kaya2", "kaya2.jpg".some),
    new Theme("oak", "oak.png".some),
    new Theme("blue", None),
    new Theme("gray", None),
    new Theme("painting1", "painting1.jpg".some),
    new Theme("painting2", "painting2.jpg".some),
    new Theme("kinkaku", "kinkaku.jpg".some),
    new Theme("space", "space.png".some),
    new Theme("doubutsu", "doubutsu.png".some),
    new Theme("custom", None)
  )

  lazy val allByName = all map { c =>
    c.name -> c
  } toMap

  def apply(name: String) = allByName.getOrElse(name, default)

  def contains(name: String) = allByName contains name
}

case class CustomTheme(
    boardColor: String,
    boardImg: String,
    gridColor: String,
    gridWidth: Int,
    handsColor: String,
    handsImg: String
)

object CustomTheme {
  val default = new CustomTheme(
    boardColor = "initial", // uses css fallback
    boardImg = "",
    gridColor = "initial",
    gridWidth = 1,
    handsColor = "initial",
    handsImg = ""
  )
}
