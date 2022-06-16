package lila.common

import chess.opening.{ FullOpening, FullOpeningDB, OpeningFamily, OpeningVariation }

case class LilaOpening(ref: FullOpening) {
  import LilaOpening._
  val name: Name = Name(ref.variation.fold(ref.family.name)(v => s"${ref.family.name}: ${v.name}"))
  val key: Key   = nameToKey(name)
  def variation  = ref.variation
}

object LilaOpening {

  case class Key(value: String)  extends AnyVal with StringValue
  case class Name(value: String) extends AnyVal with StringValue

  implicit val keyIso = Iso.string[Key](Key.apply, _.value)

  def nameToKey(name: Name) = Key {
    java.text.Normalizer
      .normalize(
        name.value,
        java.text.Normalizer.Form.NFD
      )                                      // split an accented letter in the base letter and the accent
      .replaceAllIn("[\u0300-\u036f]".r, "") // remove all previously split accents
      .replaceAllIn("""\s+""".r, "_")
      .replaceAllIn("""[^\w\-]+""".r, "")
  }

  def apply(key: Key): Option[LilaOpening]   = openings get key
  def find(key: String): Option[LilaOpening] = apply(Key(key))

  lazy val openings: Map[Key, LilaOpening] = FullOpeningDB.all
    .foldLeft(Map.empty[Key, LilaOpening]) { case (acc, fullOp) =>
      val op = LilaOpening(fullOp)
      if (acc.contains(op.key)) acc else acc.updated(op.key, op)
    }

  lazy val openingList = openings.values.toList.sortBy(_.name.value)
}

case class LilaOpeningFamily(family: OpeningFamily, ref: FullOpening) {
  lazy val key = LilaOpening.nameToKey(LilaOpening.Name(family.name))
}

object LilaOpeningFamily {
  import LilaOpening.{ Key, Name }

  def apply(op: LilaOpening): Option[LilaOpeningFamily] =
    families get LilaOpening.nameToKey(Name(op.ref.family.name))
  def apply(key: Key): Option[LilaOpeningFamily]   = families get key
  def find(key: String): Option[LilaOpeningFamily] = apply(Key(key))

  lazy val families: Map[Key, LilaOpeningFamily] = FullOpeningDB.all
    .foldLeft(Map.empty[Key, LilaOpeningFamily]) { case (acc, fullOp) =>
      val fam = LilaOpeningFamily(fullOp.family, fullOp)
      if (acc.contains(fam.key)) acc else acc.updated(fam.key, fam)
    }

  lazy val familyList = families.values.toList.sortBy(_.family.name)
}
