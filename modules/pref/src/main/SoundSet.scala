package lila.pref

final class SoundSet private (val key: String, val name: String):

  override def toString = key

  def cssClass = key

object SoundSet:

  val default = new SoundSet("standard", "Standard")
  val silent = new SoundSet("silent", "Silent")

  val list = List(
    silent,
    default,
    new SoundSet("piano", "Piano"),
    new SoundSet("nes", "NES"),
    new SoundSet("sfx", "SFX"),
    new SoundSet("futuristic", "Futuristic"),
    new SoundSet("lisp", "Lisp"),
    new SoundSet("woodland", "WoodLand"),
    new SoundSet("robot", "Robot"),
    new SoundSet("music", "Pentatonic"),
    new SoundSet("speech", "Speech")
  )

  val allByKey = list.mapBy(_.key)
  val allByName = list.mapBy(_.name)

  def apply(key: String) = allByKey.getOrElse(key.toLowerCase, default)

  def contains(key: String) = allByKey contains key.toLowerCase

  def name2key(name: String): String =
    allByName.get(name).fold(name.toLowerCase)(_.key)
