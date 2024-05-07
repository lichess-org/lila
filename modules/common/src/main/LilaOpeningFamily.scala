package lila.common

import chess.opening.{ Opening, OpeningDb, OpeningFamily }

// Includes synthetic families without a concrete opening
// Examples:
// - King's Gambit Declined
// - Barnes Opening
case class LilaOpeningFamily(ref: OpeningFamily, fullOrSubstitute: Either[Opening, Opening]):
  import LilaOpeningFamily.*
  val name                = ref.name.into(Name)
  def key                 = ref.key.into(Key)
  def as(color: Color)    = LilaOpeningFamily.AsColor(this, color)
  def full                = fullOrSubstitute.left.toOption
  def substitute          = fullOrSubstitute.toOption
  def anyOpening: Opening = fullOrSubstitute.fold(identity, identity)

object LilaOpeningFamily:

  opaque type Key = String
  object Key extends OpaqueString[Key]

  opaque type Name = String
  object Name extends OpaqueString[Name]

  case class AsColor(family: LilaOpeningFamily, color: Color)

  def apply(key: Key): Option[LilaOpeningFamily]   = families.get(key)
  def find(key: String): Option[LilaOpeningFamily] = apply(Key(key))

  lazy val families: Map[Key, LilaOpeningFamily] = OpeningDb.all
    .foldLeft(Map.empty[Key, LilaOpeningFamily]): (acc, fullOp) =>
      val fam =
        LilaOpeningFamily(fullOp.family, if fullOp.variation.isEmpty then Left(fullOp) else Right(fullOp))
      acc.get(fam.key) match
        case Some(LilaOpeningFamily(_, Right(sub))) =>
          if fam.full.isDefined then acc.updated(fam.key, fam)
          else if fam.substitute.exists(_.nbMoves < sub.nbMoves) then acc.updated(fam.key, fam)
          else acc
        case Some(_) => acc
        case None    => acc.updated(fam.key, fam)

  lazy val familyList = families.values.toList.sortBy(_.name.value)
