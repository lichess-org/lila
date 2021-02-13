package lila.pref

sealed class Theme private[pref] (val name: String, val colors: Theme.HexColors) {

  override def toString = name

  def cssClass = name

  def light = colors._1
  def dark  = colors._2
}

sealed trait ThemeObject {

  val all: List[Theme]

  val default: Theme

  lazy val allByName = all map { c =>
    c.name -> c
  } toMap

  def apply(name: String) = allByName.getOrElse(name, default)

  def contains(name: String) = allByName contains name
}

object Theme extends ThemeObject {

  case class HexColor(value: String) extends AnyVal with StringValue
  type HexColors = (HexColor, HexColor)

  private[pref] val defaultHexColors = (HexColor("b0b0b0"), HexColor("909090"))

  private val colors: Map[String, HexColors] = Map(
    "blue"   -> (HexColor("dee3e6") -> HexColor("8ca2ad")),
    "brown"  -> (HexColor("f0d9b5") -> HexColor("b58863")),
    "green"  -> (HexColor("ffffdd") -> HexColor("86a666")),
    "purple" -> (HexColor("9f90b0") -> HexColor("7d4a8d")),
    "ic"     -> (HexColor("ececec") -> HexColor("c1c18e")),
    "horsey" -> (HexColor("f1d9b6") -> HexColor("8e6547"))
  )

  val all = List(
    "blue",
    "blue2",
    "blue3",
    "blue-marble",
    "canvas",
    "wood",
    "wood2",
    "wood3",
    "wood4",
    "maple",
    "maple2",
    "brown",
    "leather",
    "green",
    "marble",
    "green-plastic",
    "grey",
    "metal",
    "olive",
    "newspaper",
    "purple",
    "purple-diag",
    "pink",
    "ic",
    "horsey"
  ) map { name =>
    new Theme(name, colors.getOrElse(name, defaultHexColors))
  }

  lazy val default = allByName get "brown" err "Can't find default theme D:"
}

object Theme3d extends ThemeObject {

  val all = List(
    "Black-White-Aluminium",
    "Brushed-Aluminium",
    "China-Blue",
    "China-Green",
    "China-Grey",
    "China-Scarlet",
    "Classic-Blue",
    "Gold-Silver",
    "Light-Wood",
    "Power-Coated",
    "Rosewood",
    "Marble",
    "Wax",
    "Jade",
    "Woodi"
  ) map { name =>
    new Theme(name, Theme.defaultHexColors)
  }

  lazy val default = allByName get "Woodi" err "Can't find default theme D:"
}
