package lila.pref

import play.api.libs.json.*
import lila.common.Json.given

opaque type Featured = Boolean
object Featured extends YesNo[Featured]

case class PieceSet private[pref] (name: String, featured: Featured = Featured.No):

  override def toString = name

sealed trait PieceSetObject:
  given Writes[PieceSet] = Json.writes[PieceSet]

  def default = all.head
  val all: List[PieceSet]

  lazy val allByName = all.mapBy(_.name)

  def get(name: String): PieceSet = allByName.getOrElse(name, default)
  def get(name: Option[String]): PieceSet = name.fold(default)(get)

  def contains(name: String) = allByName contains name

object PieceSet extends PieceSetObject:

  val all = List(
    PieceSet("cburnett", Featured.Yes),
    PieceSet("merida", Featured.Yes),
    PieceSet("alpha", Featured.Yes),
    PieceSet("pirouetti"),
    PieceSet("chessnut"),
    PieceSet("chess7"),
    PieceSet("reillycraig"),
    PieceSet("companion", Featured.Yes),
    PieceSet("riohacha"),
    PieceSet("kosal", Featured.Yes),
    PieceSet("leipzig"),
    PieceSet("fantasy"),
    PieceSet("spatial"),
    PieceSet("celtic"),
    PieceSet("california"),
    PieceSet("caliente", Featured.Yes),
    PieceSet("pixel"),
    PieceSet("firi"),
    PieceSet("rhosgfx", Featured.Yes),
    PieceSet("maestro", Featured.Yes),
    PieceSet("fresca", Featured.Yes),
    PieceSet("cardinal", Featured.Yes),
    PieceSet("gioco", Featured.Yes),
    PieceSet("tatiana"),
    PieceSet("staunty", Featured.Yes),
    PieceSet("cooke"),
    PieceSet("monarchy", Featured.Yes),
    PieceSet("papercut"),
    PieceSet("governor"),
    PieceSet("dubrovny", Featured.Yes),
    PieceSet("shahi-ivory-brown"),
    PieceSet("icpieces"),
    PieceSet("mpchess", Featured.Yes),
    PieceSet("kiwen-suwi"),
    PieceSet("totoy"),
    PieceSet("horsey", Featured.Yes),
    PieceSet("anarcandy", Featured.Yes),
    PieceSet("xkcd"),
    PieceSet("shapes"),
    PieceSet("letter"),
    PieceSet("disguised")
  )

object PieceSet3d extends PieceSetObject:

  val all = List(
    PieceSet("Basic", Featured.Yes),
    PieceSet("Wood", Featured.Yes),
    PieceSet("Metal"),
    PieceSet("RedVBlue", Featured.Yes),
    PieceSet("ModernJade"),
    PieceSet("ModernWood", Featured.Yes),
    PieceSet("Glass"),
    PieceSet("Trimmed", Featured.Yes),
    PieceSet("Experimental", Featured.Yes),
    PieceSet("Staunton", Featured.Yes),
    PieceSet("CubesAndPi", Featured.Yes)
  )
