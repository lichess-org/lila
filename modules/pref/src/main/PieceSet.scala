package lila.pref

final class PieceSet private[pref] (val name: String):

  override def toString = name

  def cssClass = name

sealed trait PieceSetObject:

  val default: PieceSet
  val all: List[PieceSet]

  lazy val allByName = all.mapBy(_.name)

  def get(name: String): PieceSet         = allByName.getOrElse(name, default)
  def get(name: Option[String]): PieceSet = name.fold(default)(get)

  def contains(name: String) = allByName contains name

object PieceSet extends PieceSetObject:

  val default = PieceSet("cburnett")

  val all = List(
    default,
    PieceSet("merida"),
    PieceSet("alpha"),
    PieceSet("pirouetti"),
    PieceSet("chessnut"),
    PieceSet("chess7"),
    PieceSet("reillycraig"),
    PieceSet("companion"),
    PieceSet("riohacha"),
    PieceSet("kosal"),
    PieceSet("leipzig"),
    PieceSet("fantasy"),
    PieceSet("spatial"),
    PieceSet("celtic"),
    PieceSet("california"),
    PieceSet("caliente"),
    PieceSet("pixel"),
    PieceSet("rhosgfx"),
    PieceSet("maestro"),
    PieceSet("fresca"),
    PieceSet("cardinal"),
    PieceSet("gioco"),
    PieceSet("tatiana"),
    PieceSet("staunty"),
    PieceSet("cooke"),
    PieceSet("monarchy"),
    PieceSet("governor"),
    PieceSet("dubrovny"),
    PieceSet("icpieces"),
    PieceSet("mpchess"),
    PieceSet("kiwen-suwi"),
    PieceSet("horsey"),
    PieceSet("anarcandy"),
    PieceSet("xkcd"),
    PieceSet("shapes"),
    PieceSet("letter"),
    PieceSet("disguised")
  )

object PieceSet3d extends PieceSetObject:

  val default = PieceSet("Basic")

  val all = List(
    default,
    PieceSet("Wood"),
    PieceSet("Metal"),
    PieceSet("RedVBlue"),
    PieceSet("ModernJade"),
    PieceSet("ModernWood"),
    PieceSet("Glass"),
    PieceSet("Trimmed"),
    PieceSet("Experimental"),
    PieceSet("Staunton"),
    PieceSet("CubesAndPi")
  )
