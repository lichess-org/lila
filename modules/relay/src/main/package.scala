package lidraughts

package object relay extends PackageObject {

  private[relay] val logger = lidraughts.log("relay")

  private[relay] type RelayGames = List[RelayGame]
}
