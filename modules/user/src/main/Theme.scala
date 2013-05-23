package lila.user

sealed class Theme private (val name: String) {

  override def toString = name

  def cssClass = name 
}

object Theme extends scalaz.NonEmptyLists {

  val all = nel("brown", "blue", "green", "grey", "wood", "canvas") map {
      case name ⇒ new Theme(name)
    }

  val list = all.list

  val allByName = list map { c ⇒ c.name -> c } toMap

  val default = all.head

  def apply(name: String) = (allByName get name) | default

  def contains(name: String) = allByName contains name
}
