package lila.chess

sealed abstract class Variant(val id: Int) {

  lazy val name = toString.toLowerCase

  def standard = this == Variant.Standard

  def exotic = !standard
}

object Variant {

  case object Standard extends Variant(1)
  case object Chess960 extends Variant(2)

  val all = List(Standard, Chess960)

  val byId = all map { v ⇒ (v.id, v) } toMap

  val default = Standard

  def apply(id: Int): Option[Variant] = byId get id

  def orDefault(id: Int): Variant = apply(id) | default

  def exists(id: Int): Boolean = byId contains id
}
