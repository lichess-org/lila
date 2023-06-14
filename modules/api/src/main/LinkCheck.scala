package lila.api

import lila.chat.UserLine
import lila.common.config.NetDomain
import lila.hub.actorApi.shutup.PublicSource
import lila.simul.Simul
import lila.simul.SimulApi
import lila.swiss.Swiss
import lila.swiss.SwissApi
import lila.team.Team
import lila.team.TeamRepo
import lila.tournament.Tournament
import lila.tournament.TournamentRepo
import lila.study.Study
import lila.study.StudyRepo
import scala.annotation.nowarn

/* Determine if a link to a lichess resource
 * can be posted from another lichess resource.
 * Owners of a resource can post any link on it (but not to it).
 * Links to a team resource can be posted from another resource of the same team.
 * Links to official resources can be posted from anywhere.
 * */
final private class LinkCheck(
    domain: NetDomain,
    teamRepo: TeamRepo,
    tournamentRepo: TournamentRepo,
    simulApi: SimulApi,
    swissApi: SwissApi,
    studyRepo: StudyRepo
)(using Executor):

  import LinkCheck.*

  def apply(line: UserLine, source: PublicSource): Fu[Boolean] =
    if (multipleLinks find line.text) fuFalse
    else
      line.text match
        case tournamentLinkR(id) => withSource(source, tourLink)(id, line)
        case simulLinkR(id)      => withSource(source, simulLink)(id, line)
        case swissLinkR(id)      => withSource(source, swissLink)(id, line)
        case studyLinkR(id)      => withSource(source, studyLink)(id, line)
        case teamLinkR(id)       => withSource(source, teamLink)(id, line)
        case _                   => fuTrue

  private def withSource(
      source: PublicSource,
      f: (String, FullSource) => Fu[Boolean]
  )(id: String, line: UserLine): Fu[Boolean] = {
    source match
      case PublicSource.Tournament(id) => tournamentRepo byId id map2 FullSource.TournamentSource.apply
      case PublicSource.Simul(id)      => simulApi find id map2 FullSource.SimulSource.apply
      case PublicSource.Swiss(id)      => swissApi fetchByIdNoCache id map2 FullSource.SwissSource.apply
      case PublicSource.Team(id)       => teamRepo byId id map2 FullSource.TeamSource.apply
      case PublicSource.Study(id)      => studyRepo byId id map2 FullSource.StudySource.apply
      case _                           => fuccess(none)
  } flatMapz { source =>
    // the owners of a chat can post whichever link they like
    if (source.owners(line.userId)) fuTrue
    else f(id, source)
  }

  private def tourLink(tourId: String, source: FullSource): Fu[Boolean] =
    tournamentRepo byId TourId(tourId) flatMapz { tour =>
      fuccess(tour.isScheduled) >>| {
        source.teamId so { sourceTeamId =>
          fuccess(tour.conditions.teamMember.exists(_.teamId == sourceTeamId)) >>|
            tournamentRepo.isForTeam(tour.id, sourceTeamId)
        }
      }
    }

  private def simulLink(simulId: String, source: FullSource) =
    simulApi teamOf SimulId(simulId) map {
      _ exists source.teamId.has
    }

  private def swissLink(swissId: String, source: FullSource) =
    swissApi teamOf SwissId(swissId) map {
      _ exists source.teamId.has
    }

  private def studyLink(@nowarn studyId: String, @nowarn source: FullSource) = fuFalse

  private def teamLink(@nowarn teamId: String, @nowarn source: FullSource) = fuFalse

  private val multipleLinks   = s"(?i)$domain.+$domain".r.unanchored
  private val tournamentLinkR = s"(?i)$domain/tournament/(\\w{8})".r.unanchored
  private val simulLinkR      = s"(?i)$domain/simul/(\\w{8})".r.unanchored
  private val swissLinkR      = s"(?i)$domain/swiss/(\\w{8})".r.unanchored
  private val studyLinkR      = s"(?i)$domain/study/(\\w{8})".r.unanchored
  private val teamLinkR       = s"(?i)$domain/team/([\\w-]+)".r.unanchored

private object LinkCheck:

  sealed trait FullSource:
    def owners: Set[UserId]
    def teamId: Option[TeamId]

  object FullSource:
    case class TournamentSource(value: Tournament) extends FullSource:
      def owners = Set(value.createdBy)
      def teamId = value.conditions.teamMember.map(_.teamId)
    case class SimulSource(value: Simul) extends FullSource:
      def owners = Set(value.hostId)
      def teamId = value.conditions.teamMember.map(_.teamId)
    case class SwissSource(value: Swiss) extends FullSource:
      def owners = Set(value.createdBy)
      def teamId = value.teamId.some
    case class TeamSource(value: Team) extends FullSource:
      def owners = value.leaders
      def teamId = value.id.some
    case class StudySource(value: Study) extends FullSource:
      def owners = value.members.idSet
      def teamId = none
