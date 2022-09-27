package lila.common

import chess.opening.{ FullOpening, FullOpeningDB, OpeningVariation }
import chess.opening.FullOpening.nameToKey

/*
 * Simple openings only keep one level of variation.
 * They're also unique by key:
 * there's only one "Sicilian Defense: Smith-Morra Gambit Accepted" SimpleOpening,
 * even tho there are multiple FullOpening with that name.
 */

case class SimpleOpening(ref: FullOpening, name: SimpleOpening.Name, family: LilaOpeningFamily) {
  import SimpleOpening._
  val key            = Key(nameToKey(name.value))
  def isFamily       = ref.variation.isEmpty
  def familyKeyOrKey = if (isFamily) Key(family.key.value) else key
  def variation      = ref.variation | otherVariations
  lazy val nbMoves   = ref.uci.count(' ' ==) + 1
  lazy val lastUci   = ref.uci.split(' ').lastOption
}

object SimpleOpening {

  case class Key(value: String)  extends AnyVal with StringValue
  case class Name(value: String) extends AnyVal with StringValue

  val otherVariations = OpeningVariation("Other variations")

  def apply(key: Key): Option[SimpleOpening]         = openings get key
  def apply(ref: FullOpening): Option[SimpleOpening] = openings get Key(nameToKey(nameOf(ref).value))

  def find(key: String): Option[SimpleOpening] = apply(Key(key))

  def nameOf(ref: FullOpening) = Name(s"${ref.family.name}: ${(ref.variation | otherVariations).name}")

  lazy val openings: Map[Key, SimpleOpening] = FullOpeningDB.all
    .foldLeft(Map.empty[Key, SimpleOpening]) { case (acc, ref) =>
      LilaOpeningFamily.find(ref.family.key).fold(acc) { fam =>
        val op   = SimpleOpening(ref, nameOf(ref), fam)
        val prev = acc get op.key
        if (prev.fold(true)(_.nbMoves > op.nbMoves)) acc.updated(op.key, op)
        else acc
      }
    }

  lazy val openingList = openings.values.toList.sortBy(_.name.value)

  implicit val keyIso = Iso.string[Key](Key.apply, _.value)
}
