package lila
package team

import core.Settings
import site.Captcha
import user.UserRepo

import com.mongodb.casbah.MongoCollection

final class TeamEnv(
    settings: Settings,
    captcha: Captcha,
    userRepo: UserRepo,
    mongodb: String ⇒ MongoCollection) {

  import settings._

  lazy val teamRepo = new TeamRepo(mongodb(TeamCollectionTeam))

  lazy val memberRepo = new MemberRepo(mongodb(TeamCollectionMember))

  lazy val paginator = new PaginatorBuilder(
    memberRepo = memberRepo,
    teamRepo = teamRepo,
    userRepo = userRepo,
    maxPerPage = TeamPaginatorMaxPerPage)

  lazy val api = new TeamApi(
    teamRepo = teamRepo,
    memberRepo = memberRepo,
    userRepo = userRepo,
    paginator = paginator)

  lazy val forms = new DataForm(teamRepo, captcha)
}
