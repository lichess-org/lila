package lila.pref

import scalaz.NonEmptyList

sealed class PieceSet private (val name: String) {

  override def toString = name

  def cssClass = name
}

object PieceSet {

  val all = NonEmptyList("cburnett", "merida", "pirouetti", "alpha", "spatial") map { name => new PieceSet(name) }

  val list = all.list

  val listString = list mkString " "

  val allByName = list map { c => c.name -> c } toMap

  val default = all.head

  def apply(name: String) = (allByName get name) | default

  def contains(name: String) = allByName contains name
}
