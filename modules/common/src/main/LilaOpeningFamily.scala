package lila.common

import chess.Color
import chess.opening.{ FullOpening, FullOpeningDB, OpeningFamily }

case class LilaOpeningFamily(ref: OpeningFamily, full: Option[FullOpening]) {
  import LilaOpeningFamily._
  val name             = Name(ref.name)
  def key              = Key(ref.key)
  def as(color: Color) = LilaOpeningFamily.AsColor(this, color)
}

object LilaOpeningFamily {

  case class Key(value: String)  extends AnyVal with StringValue
  case class Name(value: String) extends AnyVal with StringValue
  case class AsColor(family: LilaOpeningFamily, color: Color)

  def apply(key: Key): Option[LilaOpeningFamily]   = families get key
  def find(key: String): Option[LilaOpeningFamily] = apply(Key(key))

  lazy val families: Map[Key, LilaOpeningFamily] = FullOpeningDB.all
    .foldLeft(Map.empty[Key, LilaOpeningFamily]) { case (acc, fullOp) =>
      val fam = LilaOpeningFamily(fullOp.family, fullOp.variation.isEmpty option fullOp)
      acc.get(fam.key) match {
        case Some(LilaOpeningFamily(_, None)) if fam.full.isDefined => acc.updated(fam.key, fam)
        case Some(_)                                                => acc
        case None                                                   => acc.updated(fam.key, fam)
      }
    }

  lazy val familyList = families.values.toList.sortBy(_.name.value)

  implicit val keyIso = Iso.string[Key](Key.apply, _.value)
}
