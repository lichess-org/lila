package lidraughts

package object user extends PackageObject {

  private[user] def logger = lidraughts.log("user")

  type Trophies = List[Trophy]
}
