package lila

package object relay extends PackageObject {

  private[relay] val logger = lila.log("relay")

  private[relay] type RelayGames = Vector[RelayGame]
}
