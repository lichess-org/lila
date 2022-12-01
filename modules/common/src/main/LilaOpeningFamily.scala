package lila.common

import chess.Color
import chess.opening.{ Opening, OpeningDb, OpeningFamily }

// Includes synthetic families without a concrete opening
// Examples:
// - King's Gambit Declined
// - Barnes Opening
case class LilaOpeningFamily(ref: OpeningFamily, full: Option[Opening]):
  import LilaOpeningFamily.*
  val name             = ref.name into Name
  def key              = ref.key into Key
  def as(color: Color) = LilaOpeningFamily.AsColor(this, color)

object LilaOpeningFamily:

  opaque type Key = String
  object Key extends OpaqueString[Key]

  opaque type Name = String
  object Name extends OpaqueString[Name]

  case class AsColor(family: LilaOpeningFamily, color: Color)

  def apply(key: Key): Option[LilaOpeningFamily]   = families get key
  def find(key: String): Option[LilaOpeningFamily] = apply(Key(key))

  lazy val families: Map[Key, LilaOpeningFamily] = OpeningDb.all
    .foldLeft(Map.empty[Key, LilaOpeningFamily]) { case (acc, fullOp) =>
      val fam = LilaOpeningFamily(fullOp.family, fullOp.variation.isEmpty option fullOp)
      acc.get(fam.key) match
        case Some(LilaOpeningFamily(_, None)) if fam.full.isDefined => acc.updated(fam.key, fam)
        case Some(_)                                                => acc
        case None                                                   => acc.updated(fam.key, fam)
    }

  lazy val familyList = families.values.toList.sortBy(_.name.value)
