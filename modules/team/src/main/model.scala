package lila.team

final class GetTeamName(f: (Team.ID => Option[String])) extends (Team.ID => Option[String]) {
  def apply(id: Team.ID) = f(id)
}
