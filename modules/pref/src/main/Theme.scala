package lila.pref

sealed class Theme private[pref] (val name: String) {

  override def toString = name

  def cssClass = name
}

object Theme {

  val all = List(
    "solid-orange",
    "solid-natural",
    "wood1",
    "kaya1",
    "kaya2",
    "oak",
    "blue",
    "gray",
    "Painting1",
    "Painting2",
    "Kinkaku",
    "space",
    "doubutsu",
    "custom",
  ) map { name =>
    new Theme(name)
  }

  lazy val default = allByName get "wood1" err "Can't find default theme D:"

  lazy val allByName = all map { c =>
    c.name -> c
  } toMap

  def apply(name: String) = allByName.getOrElse(name, default)

  def contains(name: String) = allByName contains name
}

