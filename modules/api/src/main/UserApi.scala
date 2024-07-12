package lila.api

import play.api.i18n.Lang
import play.api.libs.json.*

import lila.common.Json.given
import lila.core.config.*
import lila.rating.UserRankMap
import lila.core.perm.Granter
import lila.user.Trophy
import lila.rating.PerfType
import lila.core.perf.UserWithPerfs
import lila.core.LightUser

final class UserApi(
    jsonView: lila.user.JsonView,
    relationApi: lila.relation.RelationApi,
    bookmarkApi: lila.bookmark.BookmarkApi,
    crosstableApi: lila.game.CrosstableApi,
    gameCache: lila.game.Cached,
    userApi: lila.user.UserApi,
    userRepo: lila.user.UserRepo,
    userCache: lila.user.Cached,
    prefApi: lila.pref.PrefApi,
    streamerApi: lila.streamer.StreamerApi,
    liveStreamApi: lila.streamer.LiveStreamApi,
    gameProxyRepo: lila.round.GameProxyRepo,
    trophyApi: lila.user.TrophyApi,
    shieldApi: lila.tournament.TournamentShieldApi,
    revolutionApi: lila.tournament.RevolutionApi,
    challengeGranter: lila.challenge.ChallengeGranter,
    net: NetConfig
)(using Executor, lila.core.i18n.Translator):

  def one(u: UserWithPerfs | LightUser, joinedAt: Option[Instant] = None): JsObject = {
    val (light, userJson) = u match
      case u: UserWithPerfs => (u.user.light, jsonView.full(u.user, u.perfs.some, withProfile = false))
      case u: LightUser     => (u, Json.toJsObject(u))
    addStreaming(userJson, light.id) ++
      Json.obj("url" -> makeUrl(s"@/${light.name}")) // for app BC
  }.add("joinedTeamAt", joinedAt)

  def extended(
      username: UserStr,
      withFollows: Boolean,
      withTrophies: Boolean,
      withCanChallenge: Boolean
  )(using Option[Me], Lang): Fu[Option[JsObject]] =
    userApi.withPerfs(username).flatMapz {
      extended(_, withFollows, withTrophies, withCanChallenge).dmap(some)
    }

  def extended(
      u: User | UserWithPerfs,
      withFollows: Boolean,
      withTrophies: Boolean,
      withCanChallenge: Boolean,
      forWiki: Boolean = false
  )(using as: Option[Me], lang: Lang): Fu[JsObject] =
    u.match
      case u: User          => userApi.withPerfs(u)
      case u: UserWithPerfs => fuccess(u)
    .flatMap: u =>
        if u.enabled.no
        then fuccess(jsonView.disabled(u.light))
        else
          (
            gameProxyRepo.urgentGames(u).dmap(_.headOption),
            as.filter(u !=).so(me => crosstableApi.nbGames(me.userId, u.id)),
            withFollows.soFu(relationApi.countFollowing(u.id)),
            as.isDefined.so(prefApi.followable(u.id)),
            as.map(_.userId).so(relationApi.fetchRelation(_, u.id)),
            as.map(_.userId).so(relationApi.fetchFollows(u.id, _)),
            bookmarkApi.countByUser(u.user),
            gameCache.nbPlaying(u.id),
            gameCache.nbImportedBy(u.id),
            (withTrophies && !u.lame).soFu(getTrophiesAndAwards(u.user)),
            streamerApi.listed(u.user),
            withCanChallenge.so(challengeGranter.mayChallenge(u.user).dmap(some)),
            forWiki.soFu(userRepo.email(u.id))
          ).mapN:
            (
                gameOption,
                nbGamesWithMe,
                following,
                followable,
                relation,
                isFollowed,
                nbBookmarks,
                nbPlaying,
                nbImported,
                trophiesAndAwards,
                streamer,
                canChallenge,
                email
            ) =>
              jsonView.full(u.user, u.perfs.some, withProfile = true) ++ {
                Json
                  .obj(
                    "url"     -> makeUrl(s"@/${u.username}"), // for app BC
                    "playing" -> gameOption.map(g => makeUrl(s"${g.gameId}/${g.color.name}")),
                    "count" -> Json.obj(
                      "all"      -> u.count.game,
                      "rated"    -> u.count.rated,
                      "ai"       -> u.count.ai,
                      "draw"     -> u.count.draw,
                      "drawH"    -> u.count.drawH,
                      "loss"     -> u.count.loss,
                      "lossH"    -> u.count.lossH,
                      "win"      -> u.count.win,
                      "winH"     -> u.count.winH,
                      "bookmark" -> nbBookmarks,
                      "playing"  -> nbPlaying,
                      "import"   -> nbImported,
                      "me"       -> nbGamesWithMe
                    )
                  )
                  .add("email", email)
                  .add("groups", forWiki.option(wikiGroups(u.user)))
                  .add("streaming", liveStreamApi.isStreaming(u.id))
                  .add("nbFollowing", following)
                  .add("nbFollowers", withFollows.option(0))
                  .add("trophies", trophiesAndAwards.map(trophiesJson))
                  .add("canChallenge", canChallenge)
                  .add(
                    "streamer",
                    streamer.map: s =>
                      Json
                        .obj()
                        .add(
                          "twitch",
                          s.twitch.map: t =>
                            Json.obj("channel" -> t.fullUrl)
                        )
                        .add(
                          "youTube",
                          s.youTube.map: y =>
                            Json.obj("channel" -> y.fullUrl)
                        )
                  ) ++
                  as.isDefined.so:
                    Json.obj(
                      "followable" -> followable,
                      "following"  -> relation.exists(_.isFollow),
                      "blocking"   -> relation.exists(!_.isFollow),
                      "followsYou" -> isFollowed
                    )
              }.noNull

  def getTrophiesAndAwards(u: User) =
    (trophyApi.findByUser(u), shieldApi.active(u), revolutionApi.active(u)).mapN:
      case (trophies, shields, revols) =>
        val roleTrophies = trophyApi.roleBasedTrophies(
          u,
          Granter.ofUser(_.PublicMod)(u),
          Granter.ofUser(_.Developer)(u),
          Granter.ofUser(_.Verified)(u),
          Granter.ofUser(_.ContentTeam)(u)
        )
        UserApi.TrophiesAndAwards(userCache.rankingsOf(u.id), trophies ::: roleTrophies, shields, revols)

  private def trophiesJson(all: UserApi.TrophiesAndAwards)(using Lang): JsArray =
    JsArray {
      all.ranks.toList
        .sortBy(_._2)
        .map: (perf, rank) =>
          PerfType(perf) -> rank
        .collect {
          case (perf, rank) if rank == 1   => perfTopTrophy(perf, 1, "Champion")
          case (perf, rank) if rank <= 10  => perfTopTrophy(perf, 10, "Top 10")
          case (perf, rank) if rank <= 50  => perfTopTrophy(perf, 50, "Top 50")
          case (perf, rank) if rank <= 100 => perfTopTrophy(perf, 10, "Top 100")
        } ::: all.trophies.map { t =>
        Json
          .obj(
            "type" -> t.kind._id,
            "name" -> t.kind.name,
            "date" -> t.date
          )
          .add("icon" -> t.kind.icon)
          .add("url" -> t.anyUrl)
      }
    }

  private def perfTopTrophy(perf: PerfType, top: Int, name: String)(using Lang) = Json.obj(
    "type" -> "perfTop",
    "perf" -> perf.key,
    "top"  -> top,
    "name" -> s"${perf.trans} $name"
  )

  private def addStreaming(js: JsObject, id: UserId) =
    js.add("streaming", liveStreamApi.isStreaming(id))

  private def makeUrl(path: String): String = s"${net.baseUrl}/$path"

  private def wikiGroups(u: User): List[String] =
    val perms          = lila.security.Permission.expanded(u).map(_.name).toList
    val wikiAdminGroup = "Administrators"
    if perms.contains("Admin") then wikiAdminGroup :: perms else perms

object UserApi:
  case class TrophiesAndAwards(
      ranks: UserRankMap,
      trophies: List[Trophy],
      shields: List[lila.tournament.TournamentShield.Award],
      revolutions: List[lila.tournament.Revolution.Award]
  ):
    def countTrophiesAndPerfCups = trophies.size + ranks.count(_._2 <= 100)
