package lila.team

import lila.common.Bus
import lila.core.misc.clas.{ ClasTeamUpdate, ClasTeamConfig }
import lila.core.id.ClasId
import lila.core.team.Access

private final class TeamClasSync(api: TeamApi, memberRepo: TeamMemberRepo)(using Executor):

  Bus.sub[ClasTeamUpdate](sync(_))

  private def sync(ev: ClasTeamUpdate): Funit =
    import ev.me
    val id = ev.clasId.into(TeamId)
    api
      .team(id)
      .flatMap:
        case None => ev.wantsTeam.so(createFromClas(ev.clasId))
        case Some(team) =>
          ev.wantsTeam match
            case Some(cfg) => syncMembers(team, cfg)
            case None => deleteTeam(team)

  private def syncMembers(team: Team, cfg: ClasTeamConfig): Funit =
    import TeamSecurity.Permission.*
    val allMemberIds = cfg.teacherIds.toList ::: cfg.studentIds
    for
      _ <- allMemberIds.toList.sequentiallyVoid(api.doJoin(team, _, quietly = true))
      teacherPerms = Set(Public, Settings, Tour, Comm)
      _ <- cfg.teacherIds.toList.sequentiallyVoid: tid =>
        memberRepo.setPerms(team.id, tid, teacherPerms)
    yield ()

  private def createFromClas(clasId: ClasId)(cfg: ClasTeamConfig)(using me: Me): Funit =
    val id = clasId.into(TeamId)
    val intro = s"Team of class ${cfg.name}"
    val team = Team
      .make(
        id = id,
        name = cfg.name,
        password = None,
        intro = intro.some,
        description = Markdown(""),
        descPrivate = Markdown(s"""Team of class [${cfg.name}](${routes.Clas.show(clasId)})""").some,
        open = false,
        createdBy = me.userId
      )
      .copy(
        ofClas = true.some,
        hideMembers = true.some,
        password = scalalib.ThreadLocalRandom.nextString(12).some,
        chat = Access.None,
        forum = Access.None
      )
    for
      _ <- api.createQuietly(team)
      _ <- syncMembers(team, cfg)
    yield ()

  // it's a bit too easy to unselect the class team from the class settings
  // if it resulted in actual team deletion, we would probably get support requests
  private def deleteTeam(team: Team)(using me: Me): Funit =
    api.toggleEnabled(team, "Unselected from class settings").void
