package lila

package object user extends PackageObject with WithPlay {

  private[user] def logger = lila.log("user")

  type Trophies = List[Trophy]
}
