package lila.hub

package object lightTeam {
  type TeamId = String
  type TeamName = String
  case class LightTeam(id: TeamId, name: TeamName) {
    def pair = id -> name
  }
}
