package lila.pref

import scalaz.NonEmptyList

sealed class Theme private (val name: String) {

  override def toString = name

  def cssClass = name
}

object Theme {

  val all = NonEmptyList(
    "brown", "blue", "green", 
    "wood", "canvas", "marble",
    "wood2", "blue2", "leather",
    "olive", "grey", "purple"
  ) map { name => new Theme(name) }

  val list = all.list

  val listString = list mkString " "

  val allByName = list map { c => c.name -> c } toMap

  val default = all.head

  def apply(name: String) = (allByName get name) | default

  def contains(name: String) = allByName contains name
}
