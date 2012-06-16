package lila
package ui

sealed trait Color

object Color {

  case object blue extends Color
  case object wood extends Color
  case object grey extends Color
  case object green extends Color

  val all = List(blue, wood, grey, green)
  val allByName = all map { c ⇒
    c.toString -> c
  } toMap

  val default = blue

  def apply(name: String) = (allByName get name) | default

  def contains(name: String) = allByName contains name
}
