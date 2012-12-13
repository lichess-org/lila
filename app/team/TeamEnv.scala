package lila
package team

import core.Settings
import site.Captcha
import user.UserRepo
import message.LichessThread
import forum.{ Categ, PostView }

import com.mongodb.casbah.MongoCollection
import scalaz.effects._

final class TeamEnv(
    settings: Settings,
    captcha: Captcha,
    userRepo: UserRepo,
    sendMessage: LichessThread ⇒ IO[Unit],
    makeForum: (String, String) ⇒ IO[Unit],
    getForumNbPosts: String ⇒ IO[Int],
    getForumPosts: String ⇒ IO[List[PostView]],
    mongodb: String ⇒ MongoCollection) {

  import settings._

  lazy val teamRepo = new TeamRepo(mongodb(TeamCollectionTeam))

  lazy val memberRepo = new MemberRepo(mongodb(TeamCollectionMember))

  lazy val requestRepo = new RequestRepo(mongodb(TeamCollectionRequest))

  private lazy val messenger = new TeamMessenger(
    send = sendMessage,
    netBaseUrl = NetBaseUrl)

  lazy val paginator = new PaginatorBuilder(
    memberRepo = memberRepo,
    teamRepo = teamRepo,
    userRepo = userRepo,
    maxPerPage = TeamPaginatorMaxPerPage)

  lazy val api = new TeamApi(
    teamRepo = teamRepo,
    memberRepo = memberRepo,
    requestRepo = requestRepo,
    cached = cached,
    userRepo = userRepo,
    messenger = messenger,
    makeForum = makeForum,
    paginator = paginator)

  lazy val teamInfo = TeamInfo(
    api = api,
    memberRepo = memberRepo,
    requestRepo = requestRepo,
    userRepo = userRepo,
    getForumNbPosts = getForumNbPosts,
    getForumPosts = getForumPosts) _

  lazy val forms = new DataForm(teamRepo, captcha)

  lazy val cached = new Cached(teamRepo, memberRepo)
}
