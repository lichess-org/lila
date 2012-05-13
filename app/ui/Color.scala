package lila
package ui

sealed trait Color

object Color {

  case object brown extends Color
  case object grey extends Color
  case object blue extends Color
  case object green extends Color

  val all = List(brown, grey, blue, green)
  val allByName = all map { c =>
    c.toString -> c
  } toMap

  val default = brown

  def apply(name: String) = (allByName get name) | default

  def contains(name: String) = allByName contains name
}
