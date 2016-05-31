package lila.pref

import scalaz.NonEmptyList

sealed class SoundSet private[pref] (val key: String, val name: String) {

  override def toString = key

  def cssClass = key
}

sealed trait SoundSetObject {

  def all: NonEmptyList[SoundSet]

  lazy val list = all.list

  lazy val listString = list mkString " "

  lazy val allByKey = list map { c => c.key -> c } toMap

  lazy val default = all.head

  def apply(key: String) = allByKey.getOrElse(key, default)

  def contains(key: String) = allByKey contains key
}

object SoundSet extends SoundSetObject {

  val all = NonEmptyList(
    new SoundSet("silent", "Silent"),
    new SoundSet("standard", "Standard"),
    new SoundSet("piano", "Piano"),
    new SoundSet("nes", "NES"),
    new SoundSet("sfx", "SFX"),
    new SoundSet("futuristic", "Futuristic"),
    new SoundSet("robot", "Robot"),
    new SoundSet("music", "Music"))
}
