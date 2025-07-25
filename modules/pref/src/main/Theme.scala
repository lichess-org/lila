package lila.pref

import play.api.libs.json.*
import lila.common.Json.given

case class Theme private[pref] (name: String, file: String, featured: Featured = Featured.No):

  override def toString = name

sealed trait ThemeObject:
  given Writes[Theme] = Json.writes[Theme]

  def default = all.head
  val all: List[Theme]

  lazy val allByName = all.mapBy(_.name)

  def apply(name: String): Theme = allByName.getOrElse(name, default)
  def apply(name: Option[String]): Theme = name.fold(default)(apply)

  def contains(name: String) = allByName contains name

object Theme extends ThemeObject:

  val all = List(
    Theme("brown", "brown.png", Featured.Yes), // 52/1 poll votes [for]/[against]
    Theme("wood", "wood.jpg"), // 14/20
    Theme("wood2", "wood2.jpg"), // 7/26
    Theme("wood3", "wood3.jpg"), // 13/17
    Theme("wood4", "wood4.jpg", Featured.Yes), // 29/3
    Theme("maple", "maple.jpg", Featured.Yes), // 24/7
    Theme("maple2", "maple2.jpg"), // 9/20
    Theme("horsey", "horsey.jpg", Featured.Yes), // 20/11
    Theme("leather", "leather.jpg"), // 6/24
    Theme("blue", "blue.png", Featured.Yes), // 29/4
    Theme("blue2", "blue2.jpg", Featured.Yes), // 18/13
    Theme("blue3", "blue3.jpg", Featured.Yes), // 23/9
    Theme("canvas", "canvas2.jpg"), // 14/17
    Theme("blue-marble", "blue-marble.jpg"), // 8/23
    Theme("ic", "ic.png"), // 13/17
    Theme("green", "green.png", Featured.Yes), // 26/5
    Theme("marble", "marble.jpg", Featured.Yes), // 17/14
    Theme("green-plastic", "green-plastic.png"), // 12/20
    Theme("olive", "olive.jpg", Featured.Yes), // 21/11
    Theme("grey", "grey.jpg", Featured.Yes), // 20/13
    Theme("metal", "metal.jpg", Featured.Yes), // 17/14
    Theme("newspaper", "svg/newspaper.svg", Featured.Yes), // 19/13
    Theme("purple", "purple.png", Featured.Yes), // 19/11
    Theme("purple-diag", "purple-diag.png", Featured.Yes), // 20/11
    Theme("pink", "pink-pyramid.png") // 12/18
  )

object Theme3d extends ThemeObject:

  val all = List(
    Theme("Woodi", "Woodi.png", Featured.Yes), // 13/8
    Theme("Black-White-Aluminium", "Black-White-Aluminium.png", Featured.Yes), // 15/5
    Theme("Brushed-Aluminium", "Brushed-Aluminium.png"), // 5/16
    Theme("China-Blue", "China-Blue.png", Featured.Yes), // 11/10
    Theme("China-Green", "China-Green.png", Featured.Yes), // 18/3
    Theme("China-Grey", "China-Grey.png", Featured.Yes), // 17/4
    Theme("China-Scarlet", "China-Scarlet.png", Featured.Yes), // 13/7
    Theme("China-Yellow", "China-Yellow.png"), // 5/15
    Theme("Classic-Blue", "Classic-Blue.png"), // 5/15
    Theme("Gold-Silver", "Gold-Silver.png"), // 9/11
    Theme("Green-Glass", "Green-Glass.png"), // 1/19
    Theme("Light-Wood", "Light-Wood.png", Featured.Yes), // 18/1
    Theme("Power-Coated", "Power-Coated.png", Featured.Yes), // 11/9
    Theme("Purple-Black", "Purple-Black.png"), // 5/15
    Theme("Rosewood", "Rosewood.png", Featured.Yes), // 12/8
    Theme("Wood-Glass", "Wood-Glass.png"), // 3/16
    Theme("Marble", "Marble.png", Featured.Yes), // 14/4
    Theme("Wax", "Wax.png", Featured.Yes), // 9/11 (featured for an even 12)
    Theme("Jade", "Jade.png", Featured.Yes) // 10/9
  )
