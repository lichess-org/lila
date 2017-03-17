package lila.pref

import scalaz.NonEmptyList

sealed class Theme private[pref] (val name: String, val colors: Theme.HexColors) {

  override def toString = name

  def cssClass = name

  def light = colors._1
  def dark = colors._2
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

  case class HexColor(value: String) extends AnyVal with StringValue
  type HexColors = (HexColor, HexColor)

  private[pref] val defaultHexColors = (HexColor("8a8a8a"), HexColor("c0c0c0"))

  private val colors: Map[String, HexColors] = Map(
    "blue" -> (HexColor("dee3e6"), HexColor("8ca2ad")),
    "brown" -> (HexColor("f0d9b5"), HexColor("b58863")),
    "green" -> (HexColor("ffffdd"), HexColor("86a666")),
    "purple" -> (HexColor("9f90b0"), HexColor("7d4a8d"))
  )

  val all = NonEmptyList(
    "blue", "blue2", "blue3", "canvas",
    "wood", "wood2", "wood3", "maple",
    "green", "marble", "brown", "leather",
    "grey", "metal", "olive", "purple"
  ) map { name =>
      new Theme(name, colors.getOrElse(name, defaultHexColors))
    }

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
  ) map { name => new Theme(name, Theme.defaultHexColors) }

  lazy val default = allByName get "Woodi" err "Can't find default theme D:"
}
