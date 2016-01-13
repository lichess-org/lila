package lila.pref

import scalaz.NonEmptyList

sealed class Theme private[pref] (val name: String) {

  override def toString = name

  def cssClass = name
}

sealed trait ThemeObject {

  def all: NonEmptyList[Theme]

  def default: Theme

  lazy val list = all.list

  lazy val listString = list mkString " "

  lazy val allByName = list map { c => c.name -> c } toMap

  def apply(name: String) = (allByName get name) | default

  def contains(name: String) = allByName contains name
}

object Theme extends ThemeObject {

  val all = NonEmptyList(
    "blue", "blue2", "blue3", "canvas",
    "wood", "wood2", "wood3", "maple",
    "green", "marble", "brown", "leather",
    "grey", "metal", "olive", "purple"
  ) map { name => new Theme(name) }

  lazy val default = allByName get "brown" err "Can't find default theme D:"
}

object Theme3d extends ThemeObject {

  val all = NonEmptyList(
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
  ) map { name => new Theme(name) }

  lazy val default = allByName get "Woodi" err "Can't find default theme D:"
}
