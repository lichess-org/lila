package lila

package object user extends PackageObject {

  private[user] def logger = lila.log("user")

  type Trophies = List[Trophy]
}
