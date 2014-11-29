package lila.pref

import scalaz.NonEmptyList

sealed class Theme private[pref] (val name: String) {

  override def toString = name

  def cssClass = name
}

sealed trait ThemeObject {

  def all: NonEmptyList[Theme]

  lazy val list = all.list

  lazy val listString = list mkString " "

  lazy val allByName = list map { c => c.name -> c } toMap

  lazy val default = all.head

  def apply(name: String) = (allByName get name) | default

  def contains(name: String) = allByName contains name
}

object Theme extends ThemeObject {

  val all = NonEmptyList(
    "brown", "blue", "green",
    "wood", "blue2", "marble",
    "wood3", "blue3", "leather",
    "wood2", "canvas", "purple",
    "olive", "grey", "metal"
  ) map { name => new Theme(name) }
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
}
