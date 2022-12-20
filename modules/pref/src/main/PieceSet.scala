package lila.pref

final class PieceSet private[pref] (val name: String):

  override def toString = name

  def cssClass = name

sealed trait PieceSetObject:

  val all: List[PieceSet]
  lazy val allByName = all.mapBy(_.name)

  val default: PieceSet

  def apply(name: String): PieceSet         = allByName.getOrElse(name, default)
  def apply(name: Option[String]): PieceSet = name.fold(default)(apply)

  def contains(name: String) = allByName contains name

object PieceSet extends PieceSetObject:

  val default = PieceSet("cburnett")

  val all = List(
    default.name,
    "merida",
    "alpha",
    "pirouetti",
    "chessnut",
    "chess7",
    "reillycraig",
    "companion",
    "riohacha",
    "kosal",
    "leipzig",
    "fantasy",
    "spatial",
    "celtic",
    "california",
    "pixel",
    "maestro",
    "fresca",
    "cardinal",
    "gioco",
    "tatiana",
    "staunty",
    "governor",
    "dubrovny",
    "icpieces",
    "horsey",
    "anarcandy",
    "shapes",
    "letter",
    "disguised"
  ) map { PieceSet(_) }

object PieceSet3d extends PieceSetObject:

  val default = PieceSet("Basic")

  val all = List(
    default.name,
    "Wood",
    "Metal",
    "RedVBlue",
    "ModernJade",
    "ModernWood",
    "Glass",
    "Trimmed",
    "Experimental",
    "Staunton",
    "CubesAndPi"
  ) map { PieceSet(_) }
