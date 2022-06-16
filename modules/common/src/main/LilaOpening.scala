package lila.common

import chess.opening.{ FullOpening, FullOpeningDB, OpeningFamily, OpeningVariation }

case class LilaOpening(ref: FullOpening) {
  import LilaOpening._
  val name: Name = Name(ref.variation.fold(ref.family.name)(v => s"${ref.family.name}: ${v.name}"))
  val key: Key   = nameToKey(name)
  def family     = ref.family
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

  def apply(key: Key): Option[LilaOpening] = openings get key

  def find(key: String): Option[LilaOpening] = apply(Key(key))

  lazy val openings: Map[Key, LilaOpening] = FullOpeningDB.all
    .foldLeft(Map.empty[Key, LilaOpening]) { case (acc, fullOp) =>
      val op = LilaOpening(fullOp)
      if (acc.contains(op.key)) acc else acc.updated(op.key, op)
    }

  lazy val openingList = openings.values.toList
}
