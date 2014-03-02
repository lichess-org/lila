package lila.pref

import scalaz.NonEmptyList

sealed class Theme private (val name: String) {

  override def toString = name

  def cssClass = name
}

object Theme {

  val all = NonEmptyList(
    "brown", "blue", "green",
    "purple", "yellow", "grey",
    "wood", "canvas", "leather"
  ) map { case name => new Theme(name) }

  val list = all.list

  val allByName = list map { c => c.name -> c } toMap

  val default = all.head

  def apply(name: String) = (allByName get name) | default

  def contains(name: String) = allByName contains name
}
