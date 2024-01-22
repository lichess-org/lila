package lila.pref

sealed class Theme private[pref] (val key: String, val name: String, val file: Option[String]) {

  override def toString = key

  def cssClass = key
}

object Theme {

  val default = new Theme("wood", "Wood", "wood.png".some)

  val all = List(
    new Theme("orange", "Orange", None),
    new Theme("natural", "Natural", None),
    default,
    new Theme("wood1", "Wood alternative", "wood1.jpg".some),
    new Theme("kaya1", "Kaya wood", "kaya1.jpg".some),
    new Theme("kaya2", "Kaya wood 2", "kaya2.jpg".some),
    new Theme("oak", "Oak", "oak.png".some),
    new Theme("blue", "Blue", None),
    new Theme("gray", "Gray", None),
    new Theme("painting1", "Painting 1", "painting1.jpg".some),
    new Theme("painting2", "Painting 2", "painting2.jpg".some),
    new Theme("kinkaku", "Kinkaku-ji", "kinkaku.jpg".some),
    new Theme("space", "Space", "space.png".some),
    new Theme("doubutsu", "Doubutsu", "doubutsu.png".some),
    new Theme("custom", "Custom", None)
  )

  lazy val allByKey = all map { c =>
    c.key -> c
  } toMap

  def apply(key: String) = allByKey.getOrElse(key, default)

  def contains(key: String) = allByKey contains key
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
