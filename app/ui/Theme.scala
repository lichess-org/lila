package lila
package ui

import scalaz.NonEmptyLists

sealed class Theme private (val name: String, val image: Boolean = false) {

  override def toString = name

  def cssClass = name + image.fold(" txtr", "")
}

object Theme extends NonEmptyLists {

  val all = nel(
    "brown" -> false,
    "blue" -> false,
    "green" -> false,
    "grey" -> false,
    "wood" -> true,
    "canvas" -> true
  ) map {
      case (name, image) ⇒ new Theme(name, image)
    }

  val list = all.list

  val allByName = list map { c ⇒ c.name -> c } toMap

  val default = all.head

  def apply(name: String) = (allByName get name) | default

  def contains(name: String) = allByName contains name
}
