package lila.common

import chess.Color
import chess.opening.{ FullOpening, FullOpeningDB, OpeningFamily, OpeningVariation }

case class LilaOpeningFamily(ref: OpeningFamily, full: Option[FullOpening]) {
  import LilaOpeningFamily._
  val name             = Name(ref.name)
  val key              = Key(LilaOpening nameToKey name.value)
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

case class LilaOpening(ref: FullOpening, name: LilaOpening.Name, family: LilaOpeningFamily) {
  import LilaOpening._
  val key            = Key(nameToKey(name.value))
  def isFamily       = ref.variation.isEmpty
  def familyKeyOrKey = if (isFamily) Key(family.key.value) else key
  def variation      = ref.variation | otherVariations
  lazy val nbMoves   = ref.uci.count(' ' ==) + 1
  lazy val lastUci   = ref.uci.split(' ').lastOption
}

object LilaOpening {

  case class Key(value: String)  extends AnyVal with StringValue
  case class Name(value: String) extends AnyVal with StringValue

  val otherVariations = OpeningVariation("Other variations")

  def nameToKey(name: String) =
    java.text.Normalizer
      .normalize(
        name,
        java.text.Normalizer.Form.NFD
      )                                      // split an accented letter in the base letter and the accent
      .replaceAllIn("[\u0300-\u036f]".r, "") // remove all previously split accents
      .replaceAllIn("""\s+""".r, "_")
      .replaceAllIn("""[^\w\-]+""".r, "")

  def apply(key: Key): Option[LilaOpening]         = openings get key
  def apply(ref: FullOpening): Option[LilaOpening] = openings get Key(nameToKey(nameOf(ref).value))

  def find(key: String): Option[LilaOpening] = apply(Key(key))

  def nameOf(ref: FullOpening) = Name(s"${ref.family.name}: ${(ref.variation | otherVariations).name}")

  lazy val openings: Map[Key, LilaOpening] = FullOpeningDB.all
    .foldLeft(Map.empty[Key, LilaOpening]) { case (acc, ref) =>
      LilaOpeningFamily.find(LilaOpening nameToKey ref.family.name).fold(acc) { fam =>
        val op = LilaOpening(ref, nameOf(ref), fam)
        if (acc.contains(op.key)) acc else acc.updated(op.key, op)
      }
    }

  lazy val openingList = openings.values.toList.sortBy(_.name.value)

  implicit val keyIso = Iso.string[Key](Key.apply, _.value)
}
