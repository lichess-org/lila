package lila.pref

import scalaz.NonEmptyList

sealed class PieceSet private[pref] (val name: String) {

  override def toString = name

  def cssClass = name
}

sealed trait PieceSetObject {

  def all: NonEmptyList[PieceSet]

  lazy val list = all.list

  lazy val listString = list mkString " "

  lazy val allByName = list map { c => c.name -> c } toMap

  lazy val default = all.head

  def apply(name: String) = (allByName get name) | default

  def contains(name: String) = allByName contains name
}

object PieceSet extends PieceSetObject {

  val all = NonEmptyList("cburnett", "merida", "pirouetti", "alpha", "spatial") map { name => new PieceSet(name) }
}

object PieceSet3d extends PieceSetObject {

  val all = NonEmptyList("Basic", "Glass", "Metal", "Wood", "RedVBlue", "Trimmed", "Experimental", "ModernJade") map { name => new PieceSet(name) }
}
