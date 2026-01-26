package lila.team

import lila.common.Bus
import lila.core.misc.clas.{ ClasTeamUpdate, ClasTeamConfig }
import lila.core.id.ClasId
import lila.core.team.Access

private final class TeamClasSync(
    api: TeamApi,
    teamRepo: TeamRepo,
    memberRepo: TeamMemberRepo,
    cached: TeamCached
)(using Executor)(using scheduler: Scheduler):

  private val debouncer =
    scalalib.Debouncer[ClasTeamUpdate](scheduler.scheduleOnce(3.seconds, _), 1)(sync)

  Bus.sub[ClasTeamUpdate](debouncer.push(_))

  private def sync(ev: ClasTeamUpdate): Funit =
    val id = ev.clasId.into(TeamId)
    for
      team <- api.team(id)
      _ <- (team, ev.wantsTeam) match
        case (None, None) => funit
        case (None, Some(cfg)) => ev.teacher.soUse(create(ev.clasId, cfg))
        case (Some(team), Some(cfg)) => update(team, cfg)
        case (Some(team), None) => ev.teacher.soUse(disableTeam(team))
    yield ()

  private def update(team: Team, cfg: ClasTeamConfig): Funit =
    for
      _ <- team.enabled.not.so(teamRepo.enable(team))
      _ <- syncMembers(team, cfg)
      _ <- syncPermissions(team, cfg)
    yield ()

  private def syncMembers(team: Team, cfg: ClasTeamConfig): Funit =
    for
      studentIds <- cfg.studentIds.value
      teamMemberIds <- memberRepo.userIdsByTeam(team.id)
      allClassIds = cfg.teacherIds.toList ::: studentIds
      intruders = teamMemberIds.toSet -- allClassIds.toSet
      _ <- intruders.toList.sequentiallyVoid: intruder =>
        for _ <- memberRepo.remove(team.id, intruder)
        yield cached.invalidateTeamIds(intruder)
      missing = allClassIds.toSet -- teamMemberIds.toSet
      _ <- missing.toList.sequentiallyVoid(api.doJoin(team, _, quietly = true))
      _ <- teamRepo.incMembers(team.id, missing.size - intruders.size)
    yield ()

  private def syncPermissions(team: Team, cfg: ClasTeamConfig): Funit =
    import TeamSecurity.Permission.*
    val teacherPerms = Set(Public, Settings, Tour, Comm)
    for _ <- cfg.teacherIds.toList.sequentiallyVoid: tid =>
        memberRepo.setPerms(team.id, tid, teacherPerms)
    yield ()

  private def create(clasId: ClasId, cfg: ClasTeamConfig)(using me: Me): Funit =
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
  private def disableTeam(team: Team)(using me: Me): Funit =
    team.enabled.so:
      api.toggleEnabled(team, "Unselected from class settings").void
