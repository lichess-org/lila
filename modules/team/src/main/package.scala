package lila

package object team extends PackageObject {

  private[team] def logger = lila.log("team")

  type GameTeams = chess.Color.Map[Team.ID]
}
