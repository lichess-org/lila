package lila.common

import chess.opening.{ FullOpening, FullOpeningDB, OpeningVariation, OpeningName }
import chess.opening.FullOpening.nameToKey

/*
 * Simple openings only keep one level of variation.
 * They're also unique by key:
 * there's only one "Sicilian Defense: Smith-Morra Gambit Accepted" SimpleOpening,
 * even tho there are multiple FullOpening with that name.
 */

case class SimpleOpening(ref: FullOpening, name: SimpleOpening.Name, family: LilaOpeningFamily):
  import SimpleOpening.*
  val key            = nameToKey(name into OpeningName) into Key
  def isFamily       = ref.variation.isEmpty
  def familyKeyOrKey = if (isFamily) Key(family.key.value) else key
  def variation      = ref.variation | otherVariations
  inline def nbMoves = ref.nbMoves
  inline def lastUci = ref.lastUci

object SimpleOpening:

  opaque type Key = String
  object Key extends OpaqueString[Key]

  opaque type Name = String
  object Name extends OpaqueString[Name]

  val otherVariations = OpeningVariation("Other variations")

  def apply(key: Key): Option[SimpleOpening] = openings get key
  def apply(ref: FullOpening): Option[SimpleOpening] =
    openings get nameToKey(OpeningName(nameOf(ref))).into(Key)

  def find(key: String): Option[SimpleOpening] = apply(Key(key))

  def nameOf(ref: FullOpening): Name = Name(s"${ref.family.name}: ${ref.variation | otherVariations}")

  lazy val openings: Map[Key, SimpleOpening] = FullOpeningDB.all
    .foldLeft(Map.empty[Key, SimpleOpening]) { case (acc, ref) =>
      LilaOpeningFamily(ref.family.key into LilaOpeningFamily.Key).fold(acc) { fam =>
        val op   = SimpleOpening(ref, nameOf(ref), fam)
        val prev = acc get op.key
        if (prev.fold(true)(_.nbMoves > op.nbMoves)) acc.updated(op.key, op)
        else acc
      }
    }

  lazy val openingList = openings.values.toList.sortBy(_.name.value)
