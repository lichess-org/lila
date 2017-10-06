package lila

package object relay extends PackageObject with WithPlay {

  private[relay] val logger = lila.log("relay")

  private[relay] type RelayGames = List[RelayGame]
}
