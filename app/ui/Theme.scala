package lila
package ui

import scalaz.NonEmptyLists

sealed class Theme private (val name: String) {

  override def toString = name
}

object Theme extends NonEmptyLists {

  val all = nel("brown", "blue", "wood", "grey", "green") map { name ⇒
    new Theme(name)
  }

  val list = all.list

  val allByName = list map { c ⇒ c.name -> c } toMap

  val default = all.head

  def apply(name: String) = (allByName get name) | default

  def contains(name: String) = allByName contains name
}
