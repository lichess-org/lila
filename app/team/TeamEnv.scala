package lila
package team

import core.Settings
import site.Captcha
import user.UserRepo
import message.LichessThread
import forum.PostLiteView

import com.mongodb.casbah.MongoCollection
import scalastic.elasticsearch.{ Indexer ⇒ EsIndexer }
import scalaz.effects._

final class TeamEnv(
    settings: Settings,
    esIndexer: EsIndexer,
    captcha: Captcha,
    userRepo: UserRepo,
    sendMessage: LichessThread ⇒ IO[Unit],
    makeForum: (String, String) ⇒ IO[Unit],
    getForumNbPosts: String ⇒ IO[Int],
    getForumPosts: String ⇒ IO[List[PostLiteView]],
    mongodb: String ⇒ MongoCollection) {

  import settings._

  lazy val indexer = new SearchIndexer(es = esIndexer, teamRepo = teamRepo)

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
    maxPerPage = TeamPaginatorMaxPerPage,
    maxUserPerPage = TeamPaginatorMaxUserPerPage)

  lazy val searchPaginator = new SearchPaginatorBuilder(
    indexer = indexer,
    maxPerPage = TeamPaginatorMaxPerPage)

  lazy val api = new TeamApi(
    teamRepo = teamRepo,
    memberRepo = memberRepo,
    requestRepo = requestRepo,
    cached = cached,
    userRepo = userRepo,
    messenger = messenger,
    makeForum = makeForum,
    paginator = paginator,
    indexer = indexer)

  lazy val teamInfo = TeamInfo(
    api = api,
    memberRepo = memberRepo,
    requestRepo = requestRepo,
    userRepo = userRepo,
    getForumNbPosts = getForumNbPosts,
    getForumPosts = getForumPosts) _

  lazy val forms = new DataForm(teamRepo, captcha)

  lazy val cached = new Cached(teamRepo, memberRepo, requestRepo)
}
