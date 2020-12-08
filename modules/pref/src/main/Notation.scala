package lila.pref

sealed class Notation private[pref] (val key: String, val name: String) {

  override def toString = key

  def cssClass = key
}

object Notations {

  val default = new Notation("0", "Western")

  val list = List(
    default,
    new Notation("1", "Kawasaki"),
    new Notation("2", "Japanese"),
  )

  lazy val allByKey = list map { c =>
    c.key -> c
  } toMap
  lazy val allByName = list map { c =>
    c.name -> c
  } toMap

  def apply(key: String) = allByKey.getOrElse(key.toLowerCase, default)

  def contains(key: String) = allByKey contains key.toLowerCase

  def name2key(name: String): String =
    allByName.get(name).fold(name.toLowerCase)(_.key)
}
