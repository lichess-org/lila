package lila.tournament

import lila.hub.lightTeam._

case class TeamBattle(
    teams: Set[TeamId]
)

object TeamBattle {

  object DataForm {
    import play.api.data.Forms._
    import lila.common.Form._

    val form = mapping(
      "teams" -> nonEmptyText
    )(Setup.apply)(Setup.unapply)

    case class Setup(
        teams: String
    ) {
      def potentialTeamIds: Set[String] =
        teams.lines.mkString(" ").split(" ").filter(_.nonEmpty).toSet
    }
  }
}
