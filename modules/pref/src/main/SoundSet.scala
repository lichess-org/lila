package lila.pref

sealed class SoundSet private[pref] (val key: String, val name: String) {

  override def toString = key

  def cssClass = key
}

object SoundSet {

  val default = new SoundSet("shogi", "Shogi")
  val silent  = new SoundSet("silent", "Silent")

  val list = List(
    silent,
    new SoundSet("chess", "Chess"),
    new SoundSet("nes", "NES"),
    new SoundSet("sfx", "SFX"),
    new SoundSet("futuristic", "Futuristic"),
    new SoundSet("robot", "Robot"),
    new SoundSet("music", "Pentatonic"),
    new SoundSet("shogialt", "Shogi Alternative"),
    default,
    new SoundSet("speech", "Speech")
  )

  lazy val allByKey = list map { c =>
    c.key -> c
  } toMap

  def apply(key: String) = allByKey.getOrElse(key.toLowerCase, default)

  def contains(key: String) = allByKey contains key.toLowerCase
}
