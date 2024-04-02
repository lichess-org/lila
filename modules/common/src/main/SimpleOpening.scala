package lila.common

import chess.opening.Opening.nameToKey
import chess.opening.{ Opening, OpeningDb, OpeningName, OpeningVariation }

/*
 * Simple openings only keep one level of variation.
 * They're also unique by key:
 * there's only one "Sicilian Defense: Smith-Morra Gambit Accepted" SimpleOpening,
 * even tho there are multiple Opening with that name.
 */
case class SimpleOpening(ref: Opening, name: SimpleOpening.Name, family: LilaOpeningFamily):
  import SimpleOpening.*
  val key            = nameToKey(name.into(OpeningName)).into(Key)
  def isFamily       = ref.variation.isEmpty
  def familyKeyOrKey = if isFamily then Key(family.key.value) else key
  def variation      = ref.variation | otherVariations
  inline def nbMoves = ref.nbMoves
  inline def lastUci = ref.lastUci

object SimpleOpening:

  opaque type Key = String
  object Key extends OpaqueString[Key]

  opaque type Name = String
  object Name extends OpaqueString[Name]

  val otherVariations = OpeningVariation("Other variations")

  def apply(key: Key): Option[SimpleOpening] = openings.get(key)
  def apply(ref: Opening): Option[SimpleOpening] =
    openings.get(nameToKey(OpeningName(nameOf(ref))).into(Key))

  def find(key: String): Option[SimpleOpening] = apply(Key(key))

  def nameOf(ref: Opening): Name = Name(s"${ref.family.name}: ${ref.variation | otherVariations}")

  lazy val openings: Map[Key, SimpleOpening] = OpeningDb.all
    .foldLeft(Map.empty[Key, SimpleOpening]): (acc, ref) =>
      LilaOpeningFamily(ref.family.key.into(LilaOpeningFamily.Key)).fold(acc): fam =>
        val op   = SimpleOpening(ref, nameOf(ref), fam)
        val prev = acc.get(op.key)
        if prev.forall(_.nbMoves > op.nbMoves) then acc.updated(op.key, op)
        else acc

  lazy val openingList = openings.values.toList.sortBy(_.name.value)
