package lila.pref

sealed class Theme private[pref] (val name: String) {

  override def toString = name

  def cssClass = name
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
    new Theme(name)
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
    "China-Yellow",
    "Classic-Blue",
    "Gold-Silver",
    "Green-Glass",
    "Light-Wood",
    "Power-Coated",
    "Purple-Black",
    "Rosewood",
    "Wood-Glass",
    "Marble",
    "Wax",
    "Jade",
    "Woodi"
  ) map { name =>
    new Theme(name)
  }

  lazy val default = allByName get "Woodi" err "Can't find default theme D:"
}
