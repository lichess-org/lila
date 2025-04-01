package lila.pref
import play.api.libs.json.*

enum PieceTag:
  case Featured

object PieceTag:
  given Writes[PieceTag] = Writes(tag => JsString(tag.toString))

final case class PieceSet private[pref] (val name: String, val tags: Seq[PieceTag]):

  override def toString = name

sealed trait PieceSetObject:
  given Writes[PieceSet] = Json.writes[PieceSet]

  def default = all.head
  val all: List[PieceSet]

  lazy val allByName = all.mapBy(_.name)

  def get(name: String): PieceSet         = allByName.getOrElse(name, default)
  def get(name: Option[String]): PieceSet = name.fold(default)(get)

  def contains(name: String)                         = allByName contains name
  def apply(name: String, tags: PieceTag*): PieceSet = new PieceSet(name, tags)

import PieceTag.*

object PieceSet extends PieceSetObject:

  val all = List(
    PieceSet("cburnett", Featured),  // 50/2 poll votes [for]/[against]
    PieceSet("merida", Featured),    // 28/1
    PieceSet("alpha", Featured),     // 25/5
    PieceSet("pirouetti"),           // 1/30
    PieceSet("chessnut"),            // 8/21
    PieceSet("chess7"),              // 4/25
    PieceSet("reillycraig"),         // 1/27
    PieceSet("companion", Featured), // 19/10
    PieceSet("riohacha"),            // 8/20
    PieceSet("kosal", Featured),     // 14/13
    PieceSet("leipzig"),             // 9/18
    PieceSet("fantasy"),             // 11/20
    PieceSet("spatial"),             // 6/22
    PieceSet("celtic"),              // 3/24
    PieceSet("california"),          // 12/17
    PieceSet("caliente", Featured),  // 17/11
    PieceSet("pixel"),               // 9/23
    PieceSet("rhosgfx", Featured),   // 0/0 submitted after poll
    PieceSet("maestro", Featured),   // 22/5
    PieceSet("fresca", Featured),    // 15/12
    PieceSet("cardinal", Featured),  // 15/11
    PieceSet("gioco", Featured),     // 19/10
    PieceSet("tatiana"),             // 13/15
    PieceSet("staunty", Featured),   // 21/7
    PieceSet("cooke"),               // 15/14 (misaligned)
    PieceSet("monarchy", Featured),  // 10/18 revised after poll
    PieceSet("governor"),            // 14/15
    PieceSet("dubrovny", Featured),  // 15/15
    PieceSet("icpieces"),            // 3/25
    PieceSet("mpchess", Featured),   // 17/12
    PieceSet("kiwen-suwi"),          // 8/21
    PieceSet("horsey", Featured),    // 22/8
    PieceSet("anarcandy", Featured), // 15/14
    PieceSet("xkcd"),                // 0/0 submitted after poll
    PieceSet("shapes", Featured),    // 15/15
    PieceSet("letter", Featured),    // 17/11
    PieceSet("disguised", Featured)  // 18/10
  )

object PieceSet3d extends PieceSetObject:

  val all = List(
    PieceSet("Basic", Featured),        // 12/8
    PieceSet("Wood", Featured),         // 13/4
    PieceSet("Metal"),                  // 5/12
    PieceSet("RedVBlue", Featured),     // 10/7
    PieceSet("ModernJade"),             // 4/13
    PieceSet("ModernWood", Featured),   // 10/7
    PieceSet("Glass"),                  // 5/12
    PieceSet("Trimmed", Featured),      // 10/7
    PieceSet("Experimental", Featured), // 9/9
    PieceSet("Staunton", Featured),     // 14/3
    PieceSet("CubesAndPi", Featured)    // 10/7
  )
