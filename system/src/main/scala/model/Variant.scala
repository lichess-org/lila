package lila.system
package model

sealed trait Variant {

  val name: String

  val id: Int

  override def toString = name
}

case object Standard extends Variant {

  val name = "standard"

  val id = 1
}

case object Chess960 extends Variant {

  val name = "chess960"

  val id = 2
}

object Variant {

  val all = List(Standard, Chess960)

  val byId = all map { v => (v.id, v) } toMap

  def apply(id: Int): Option[Variant] = byId get id
}
