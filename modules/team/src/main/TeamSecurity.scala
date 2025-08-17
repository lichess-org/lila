package lila
package team

import cats.derived.*

import lila.core.perm.Granter
import lila.memo.CacheApi.*

object TeamSecurity:
  enum Permission(val name: String, val desc: String) derives Eq:
    case Public extends Permission("Public", "Visible as leader on the team page")
    case Settings extends Permission("Settings", "Change settings and descriptions")
    case Tour extends Permission("Tournaments", "Create, manage and join team tournaments")
    case Comm extends Permission("Moderation", "Moderate the forum and chats")
    case Request extends Permission("Requests", "Accept and decline join requests")
    case PmAll extends Permission("Messages", "Send private messages to all members")
    case Kick extends Permission("Kick", "Kick members of the team")
    case Admin extends Permission("Admin", "Manage leader permissions")
    def key = toString.toLowerCase
  object Permission:
    type Selector = Permission.type => Permission
    val byKey = values.mapBy(_.key)

  case class LeaderData(name: UserStr, perms: Set[Permission])
  def tableStr(data: List[LeaderData]) =
    data.map(d => s"${d.name} (${d.perms.map(_.key).mkString(" ")})").mkString("\n")

  case class NewPermissions(user: UserId, perms: Set[Permission])

final class TeamSecurity(memberRepo: TeamMemberRepo, userApi: lila.core.user.UserApi, cached: Cached)(using
    Executor
):

  import TeamSecurity.*

  def setPermissions(t: Team.WithLeaders, data: List[LeaderData])(using by: Me): Fu[List[NewPermissions]] =
    for
      _ <- memberRepo.unsetAllPerms(t.id)
      _ <- memberRepo.setAllPerms(t.id, data)
    yield
      val changes = data.flatMap: d =>
        t.leaders
          .find(_.user.is(d.name))
          .filter(_.perms != d.perms && d.perms.nonEmpty)
          .map: l =>
            NewPermissions(l.user, d.perms)
      t.leaders.map(_.user).foreach(cached.nbRequests.invalidate)
      data.map(_.name.id).foreach(cached.nbRequests.invalidate)
      logger.info(s"valid setLeaders ${t.id} by ${by.userId}: ${tableStr(data)}")
      changes.toList

  def addLeader(team: Team.WithLeaders, name: UserStr): Funit =
    memberRepo.setPerms(team.id, name.id, Set(Permission.Public))

  object form:
    import play.api.data.*
    import play.api.data.Forms.*

    def addLeader(t: Team.WithLeaders)(using Me): Form[UserStr] = Form:
      single:
        "name" -> lila.common.Form.username.historicalField
          .verifying(
            s"No more than ${Team.maxLeaders} leaders, please",
            _ => t.leaders.sizeIs < Team.maxLeaders.value
          )
          .verifying(
            "You can't make Lichess a leader",
            n => Granter(_.ManageTeam) || n.isnt(UserId.lichess)
          )
          .verifying(
            "This user is already a team leader",
            n => !t.leaders.exists(_.is(n))
          )
          .verifying(
            "This user is not part of the team",
            n => memberRepo.exists(t.id, n).await(1.second, "team member exists")
          )

    private val permissionsForm = mapping(
      "name" -> lila.common.Form.username.historicalField,
      "perms" -> seq(nonEmptyText)
        .transform[Set[Permission]](
          _.flatMap(Permission.byKey.get).toSet,
          _.toList.map(_.key)
        )
    )(LeaderData.apply)(unapply)

    def permissions(t: Team.WithLeaders)(using me: Me): Form[List[LeaderData]] = Form(
      single("leaders" -> list(permissionsForm))
        .verifying(
          "You can't make Lichess a leader",
          Granter(_.ManageTeam) ||
            !_.exists(_.name.is(UserId.lichess)) ||
            t.leaders.exists(_.is(UserId.lichess))
        )
        .verifying(
          "There must be at least one leader able to manage permissions",
          _.exists(_.perms(Permission.Admin))
        )
        .verifying(
          "Illegal adding/removing leaders through permissions form",
          _.map(_.name.id).toSet == t.leaders.map(_.user).toSet
        )
        .verifying(
          s"No more than ${Team.maxLeaders} leaders, please",
          _.sizeIs <= Team.maxLeaders.value
        )
        .verifying(
          "Duplicated leader name",
          d => d.map(_.name).distinct.sizeIs == d.size
        )
        .verifying(
          "You cannot evict the team creator",
          d =>
            Granter(_.ManageTeam) || !t.hasAdminCreator || d.exists: l =>
              l.name.is(t.createdBy) && l.perms(Permission.Admin)
        )
        .verifying(
          "Kid accounts cannot manage leader permissions",
          d =>
            userApi
              .filterKid(d.filter(_.perms(Permission.Admin)).map(_.name))
              .await(1.second, "team leader kids")
              .isEmpty
        )
        .verifying(
          "There can only be 3 admins",
          _.count(_.perms(Permission.Admin)) <= 3
        )
    ).fill(t.leaders.map(m => LeaderData(m.user.into(UserStr), m.perms)))
