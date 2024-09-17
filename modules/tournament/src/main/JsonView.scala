package lila.tournament

import shogi.format.forsyth.Sfen
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import play.api.i18n.Lang
import play.api.libs.json._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.common.Json._
import lila.common.{ Animal, LightUser, Uptime }
import lila.game.{ Game, LightPov }
import lila.hub.LightTeam.TeamID
import lila.memo.CacheApi._
import lila.rating.PerfType
import lila.socket.Socket.SocketVersion
import lila.user.{ LightUserApi, User }

final class JsonView(
    lightUserApi: LightUserApi,
    playerRepo: PlayerRepo,
    pairingRepo: PairingRepo,
    tournamentRepo: TournamentRepo,
    cached: Cached,
    statsApi: TournamentStatsApi,
    shieldApi: TournamentShieldApi,
    cacheApi: lila.memo.CacheApi,
    proxyRepo: lila.round.GameProxyRepo,
    verify: Condition.Verify,
    duelStore: DuelStore,
    standingApi: TournamentStandingApi,
    pause: Pause
)(implicit ec: ExecutionContext) {

  import JsonView._

  private case class CachableData(
      duels: JsArray,
      duelTeams: Option[JsObject],
      featured: Option[JsObject],
      podium: Option[JsArray]
  )

  def apply(
      tour: Tournament,
      page: Option[Int],
      me: Option[User],
      getUserTeamIds: User.ID => Fu[List[TeamID]],
      getTeamName: TeamID => Option[String],
      playerInfoExt: Option[PlayerInfoExt],
      socketVersion: Option[SocketVersion],
      partial: Boolean
  )(implicit lang: Lang): Fu[JsObject] =
    for {
      data   <- cachableData get tour.id
      myInfo <- me ?? { fetchMyInfo(tour, _) }
      pauseDelay = me flatMap { u =>
        pause.remainingDelay(u.id, tour)
      }
      full = !partial
      stand <-
        (if (tour.isArena) (myInfo, page) match {
           case (_, Some(p)) => standingApi(tour, p)
           case (Some(i), _) => standingApi(tour, i.page)
           case _            => standingApi(tour, 1)
         }
         else cached.robin(tour.id))
      playerInfoJson <- playerInfoExt ?? { pie =>
        playerInfoExtended(tour, pie).map(_.some)
      }
      verdicts <- full ?? {
        me match {
          case None                        => fuccess(tour.conditions.accepted.some)
          case Some(_) if myInfo.isDefined => fuccess(tour.conditions.accepted.some)
          case Some(user) => verify(tour.conditions, user, tour.perfType, getUserTeamIds) map some
        }
      }
      stats       <- statsApi(tour)
      shieldOwner <- full.?? { shieldApi currentOwner tour }
      teamsToJoinWith <- full.??(~(for {
        u <- me; battle <- tour.teamBattle
      } yield getUserTeamIds(u.id) map { teams =>
        battle.teams.intersect(teams.toSet).toList
      }))
      teamStanding <- getTeamStanding(tour)
      myTeam       <- myInfo.flatMap(_.teamId) ?? { getMyRankedTeam(tour, _) }
    } yield Json
      .obj(
        "nbPlayers" -> tour.nbPlayers,
        "duels"     -> data.duels,
        "standing"  -> stand
      )
      .add("isStarted" -> tour.isStarted)
      .add("isFinished" -> tour.isFinished)
      .add("isRecentlyFinished" -> tour.isRecentlyFinished)
      .add("isClosed" -> tour.closed)
      .add("candidatesOnly" -> tour.candidatesOnly)
      .add("candidates" -> me.exists(_.id == tour.createdBy) ?? tour.candidates.some)
      .add("isCandidate" -> me ?? (m => tour.candidates.contains(m.id)))
      .add("candidatesFull" -> tour.candidatesFull)
      .add("secondsToFinish" -> tour.isStarted.option(tour.secondsToFinish))
      .add("secondsToStart" -> tour.isCreated.option(tour.secondsToStart))
      .add("me" -> myInfo.map(myInfoJson(me, pauseDelay)))
      .add("isBot" -> me.map(_.isBot))
      .add("featured" -> data.featured)
      .add("podium" -> data.podium)
      .add("playerInfo" -> playerInfoJson)
      .add("pairingsClosed" -> tour.pairingsClosed)
      .add("stats" -> stats)
      .add("socketVersion" -> socketVersion.map(_.value))
      .add("teamStanding" -> teamStanding)
      .add("myTeam" -> myTeam)
      .add("duelTeams" -> data.duelTeams) ++
      full.?? {
        Json
          .obj(
            "id"        -> tour.id,
            "createdBy" -> tour.createdBy,
            "startsAt"  -> formatDate(tour.startsAt),
            "system"    -> tour.format.key,
            "fullName"  -> tour.name(),
            "minutes"   -> tour.minutes,
            "perf"      -> full.option(tour.perfType),
            "clock"     -> full.option(tour.timeControl),
            "variant"   -> full.option(tour.variant.key),
            "rated"     -> tour.isRated
          )
          .add("spotlight" -> tour.spotlight)
          .add("berserkable" -> tour.berserkable)
          .add("position" -> tour.position.ifTrue(full).map(sfen => positionJson(sfen, tour.variant)))
          .add("verdicts" -> verdicts.map(Condition.JSONHandlers.verdictsFor(_, tour.perfType, lang)))
          .add("schedule" -> tour.schedule.map(scheduleJson))
          .add("private" -> tour.isPrivate)
          .add("proverb" -> tour.isCreated.option(lila.common.Proverb.one(tour.id)))
          .add("defender" -> shieldOwner.map(_.value))
          .add("animal" -> Animal.wikiUrl(tour.name).map { url =>
            Json.obj("name" -> tour.name, "url" -> url)
          })
          .add("teamBattle" -> tour.teamBattle.map { battle =>
            Json
              .obj(
                "teams" -> JsObject(battle.sortedTeamIds.map { id =>
                  id -> JsString(getTeamName(id).getOrElse(id))
                })
              )
              .add("joinWith" -> me.isDefined.option(teamsToJoinWith.sorted))
          })
          .add("description" -> tour.description)
      }

  def clearCache(tour: Tournament): Unit = {
    standingApi clearCache tour
    cachableData invalidate tour.id
    cached.robin invalidatePlayers tour.id
    cached.robin invalidateArrangaments tour.id
  }

  def fetchMyInfo(tour: Tournament, me: User): Fu[Option[MyInfo]] =
    playerRepo.find(tour.id, me.id) flatMap {
      _ ?? { player =>
        fetchCurrentGameId(tour, me) flatMap { gameId =>
          getOrGuessRank(tour, player) dmap { rank =>
            MyInfo(rank + 1, player.withdraw, gameId, player.team).some
          }
        }
      }
    }

  // if the user is not yet in the cached ranking,
  // guess its rank based on other players scores in the DB
  private def getOrGuessRank(tour: Tournament, player: Player): Fu[Int] =
    cached ranking tour flatMap {
      _ get player.userId match {
        case Some(rank) => fuccess(rank)
        case None       => playerRepo.computeRankOf(player)
      }
    }

  def playerInfoExtended(tour: Tournament, info: PlayerInfoExt): Fu[JsObject] =
    for {
      ranking <- cached ranking tour
      sheet   <- cached.sheet(tour, info.userId)
      user    <- lightUserApi.asyncFallback(info.userId)
    } yield info match {
      case PlayerInfoExt(_, player, povs) =>
        val isPlaying = povs.headOption.??(_.game.playable)
        val povScores: List[(LightPov, Option[arena.Sheet.Score])] = povs zip {
          (isPlaying ?? List(none[arena.Sheet.Score])) ::: sheet.scores.map(some)
        }
        Json.obj(
          "player" -> Json
            .obj(
              "id"     -> user.id,
              "name"   -> user.name,
              "rating" -> player.rating,
              "score"  -> player.score,
              "fire"   -> player.fire,
              "nb"     -> sheetNbs(sheet)
            )
            .add("title" -> user.title)
            .add("performance" -> player.performanceOption)
            .add("rank" -> ranking.get(user.id).map(1 +))
            .add("provisional" -> player.provisional)
            .add("withdraw" -> player.withdraw)
            .add("team" -> player.team),
          "pairings" -> povScores.map { case (pov, score) =>
            Json
              .obj(
                "id"     -> pov.gameId,
                "color"  -> pov.color.name,
                "op"     -> gameUserJson(pov.opponent.userId, pov.opponent.rating),
                "win"    -> score.flatMap(_.isWin),
                "status" -> pov.game.status.id,
                "score"  -> score.map(sheetScoreJson)
              )
              .add("berserk" -> pov.player.berserk)
          }
        )
    }

  private def fetchCurrentGameId(tour: Tournament, user: User): Fu[Option[Game.ID]] =
    if (tour.hasArrangements) fuccess(none) // we can get that from arrangs
    else if (Uptime.startedSinceSeconds(60)) fuccess(duelStore.find(tour, user))
    else pairingRepo.playingByTourAndUserId(tour.id, user.id)

  private def fetchFeaturedGame(tour: Tournament): Fu[Option[FeaturedGame]] =
    tour.featuredId.ifTrue(tour.isStarted) ?? proxyRepo.game flatMap {
      _ ?? { game =>
        cached ranking tour flatMap { ranking =>
          (game.twoUserIds ?? { uids => playerRepo.pairByTourAndUserIds(tour.id, uids._1, uids._2) }) map {
            pairOption =>
              for {
                (p1, p2) <- pairOption
                rp1      <- RankedPlayer(ranking)(p1)
                rp2      <- RankedPlayer(ranking)(p2)
              } yield FeaturedGame(game, rp1, rp2)
          }
        }
      }
    }

  private def sheetNbs(s: arena.Sheet) =
    Json.obj(
      "game"    -> s.scores.size,
      "berserk" -> s.scores.count(_.isBerserk),
      "win"     -> s.scores.count(_.res == arena.Sheet.ResWin)
    )

  private val cachableData = cacheApi[Tournament.ID, CachableData](16, "tournament.json.cachable") {
    _.expireAfterWrite(1 second)
      .buildAsyncFuture { id =>
        for {
          tour <- tournamentRepo byId id
          duels = duelStore.bestRated(id, 6)
          jsonDuels <- duels.map(duelJson).sequenceFu
          duelTeams <- tour.exists(_.isTeamBattle) ?? {
            playerRepo.teamsOfPlayers(id, duels.foldLeft(List.empty[User.ID])(_ ::: _.userIds)) map { teams =>
              JsObject(teams map { case (userId, teamId) =>
                (userId, JsString(teamId))
              }).some
            }
          }
          featured <- tour ?? fetchFeaturedGame
          podium   <- tour.exists(_.isFinished) ?? podiumJsonCache.get(id)
        } yield CachableData(
          duels = JsArray(jsonDuels),
          duelTeams = duelTeams,
          featured = featured map featuredJson,
          podium = podium
        )
      }
  }

  private def featuredJson(featured: FeaturedGame) = {
    val game = featured.game
    def ofPlayer(rp: RankedPlayer, p: lila.game.Player) = {
      val light = lightUserApi sync rp.player.userId
      Json
        .obj(
          "rank"   -> rp.rank,
          "name"   -> light.fold(rp.player.userId)(_.name),
          "rating" -> rp.player.rating
        )
        .add("title" -> light.flatMap(_.title))
        .add("berserk" -> p.berserk)
    }
    Json.obj(
      "id"       -> game.id,
      "sfen"     -> game.situation.toSfen,
      "color"    -> game.firstColor.name,
      "lastMove" -> ~game.lastUsiStr,
      "variant"  -> game.variant.key,
      "sente"    -> ofPlayer(featured.sente, game player shogi.Sente),
      "gote"     -> ofPlayer(featured.gote, game player shogi.Gote)
    )
  }

  private def myInfoJson(u: Option[User], delay: Option[Pause.Delay])(i: MyInfo) =
    Json
      .obj(
        "rank"     -> i.rank,
        "withdraw" -> i.withdraw,
        "gameId"   -> i.gameId,
        "username" -> u.map(_.titleUsername)
      )
      .add("pauseDelay", delay.map(_.seconds))

  private def gameUserJson(userId: Option[String], rating: Option[Int]): JsObject = {
    val light = userId flatMap lightUserApi.sync
    Json
      .obj("rating" -> rating)
      .add("name" -> light.map(_.name))
      .add("title" -> light.flatMap(_.title))
  }

  private val podiumJsonCache = cacheApi[Tournament.ID, Option[JsArray]](32, "tournament.podiumJson") {
    _.expireAfterAccess(10 seconds)
      .maximumSize(256)
      .buildAsyncFuture { id =>
        tournamentRepo finishedById id flatMap {
          _ ?? { tour =>
            playerRepo.bestByTourWithRank(id, 3).flatMap { top3 =>
              // check that the winner is still correctly denormalized
              top3.headOption.map(_.player.userId).filter(w => tour.winnerId.fold(true)(w !=)) foreach {
                tournamentRepo.setWinnerId(tour.id, _)
              }
              top3.map { case rp @ RankedPlayer(_, player) =>
                for {
                  sheet <- cached.sheet(tour, player.userId)
                  json  <- playerJson(lightUserApi, sheet.some, rp, tour.streakable)
                } yield json ++ Json
                  .obj(
                    "nb" -> sheetNbs(sheet)
                  )
                  .add("performance" -> player.performanceOption)
              }.sequenceFu
            } map { l =>
              JsArray(l).some
            }
          }
        }
      }
  }

  private def duelPlayerJson(p: Duel.DuelPlayer): Fu[JsObject] =
    lightUserApi.async(p.name.id) map { u =>
      Json
        .obj(
          "n" -> u.fold(p.name.value)(_.name),
          "r" -> p.rating.value,
          "k" -> p.rank.value
        )
        .add("t" -> u.flatMap(_.title))
    }

  private def duelJson(d: Duel): Fu[JsObject] =
    for {
      u1 <- duelPlayerJson(d.p1)
      u2 <- duelPlayerJson(d.p2)
    } yield Json.obj(
      "id" -> d.gameId,
      "p"  -> Json.arr(u1, u2)
    )

  def getTeamStanding(tour: Tournament): Fu[Option[JsArray]] =
    tour.isTeamBattle ?? { teamStandingJsonCache get tour.id dmap some }

  def apiTeamStanding(tour: Tournament): Fu[Option[JsArray]] =
    tour.teamBattle ?? { battle =>
      if (battle.hasTooManyTeams) bigTeamStandingJsonCache get tour.id dmap some
      else teamStandingJsonCache get tour.id dmap some
    }

  private val teamStandingJsonCache = cacheApi[Tournament.ID, JsArray](4, "tournament.teamStanding") {
    _.expireAfterWrite(1 second)
      .buildAsyncFuture(fetchAndRenderTeamStandingJson(TeamBattle.displayTeams))
  }

  private val bigTeamStandingJsonCache = cacheApi[Tournament.ID, JsArray](4, "tournament.teamStanding.big") {
    _.expireAfterWrite(2 seconds)
      .buildAsyncFuture(fetchAndRenderTeamStandingJson(TeamBattle.maxTeams))
  }

  private def fetchAndRenderTeamStandingJson(max: Int)(id: Tournament.ID) =
    cached.battle.teamStanding.get(id) map { ranked =>
      JsArray(ranked take max map teamBattleRankedWrites.writes)
    }

  implicit private val teamBattleRankedWrites: Writes[TeamBattle.RankedTeam] = OWrites { rt =>
    Json.obj(
      "rank"  -> rt.rank,
      "id"    -> rt.teamId,
      "score" -> rt.score,
      "players" -> rt.leaders.map { p =>
        Json.obj(
          "user"  -> lightUserApi.sync(p.userId),
          "score" -> p.score
        )
      }
    )
  }

  private def getMyRankedTeam(tour: Tournament, teamId: TeamID): Fu[Option[TeamBattle.RankedTeam]] =
    tour.teamBattle.exists(_.hasTooManyTeams) ??
      cached.battle.teamStanding.get(tour.id) map {
        _.find(_.teamId == teamId)
      }

  private val teamInfoCache =
    cacheApi[(Tournament.ID, TeamID), Option[JsObject]](16, "tournament.teamInfo.json") {
      _.expireAfterWrite(5 seconds)
        .maximumSize(32)
        .buildAsyncFuture { case (tourId, teamId) =>
          cached.teamInfo.get(tourId -> teamId) flatMap {
            _ ?? { info =>
              lightUserApi.preloadMany(info.topPlayers.map(_.userId)) inject Json
                .obj(
                  "id"        -> teamId,
                  "nbPlayers" -> info.nbPlayers,
                  "rating"    -> info.avgRating,
                  "perf"      -> info.avgPerf,
                  "score"     -> info.avgScore,
                  "topPlayers" -> info.topPlayers.flatMap { p =>
                    lightUserApi.sync(p.userId) map { user =>
                      Json
                        .obj(
                          "name"   -> user.name,
                          "rating" -> p.rating,
                          "score"  -> p.score
                        )
                        .add("fire" -> p.fire)
                        .add("title" -> user.title)
                    }
                  }
                )
                .some
            }
          }
        }
    }

  def teamInfo(tour: Tournament, teamId: TeamID): Fu[Option[JsObject]] =
    tour.isTeamBattle ?? {
      teamInfoCache get (tour.id -> teamId)
    }
}

object JsonView {

  def top(t: TournamentTop, getLightUser: LightUser.GetterSync): JsArray =
    JsArray {
      t.value.map { p =>
        val light = getLightUser(p.userId)
        Json
          .obj(
            "n" -> light.fold(p.userId)(_.name),
            "s" -> p.score
          )
          .add("t" -> light.flatMap(_.title))
          .add("f" -> p.fire)
          .add("w" -> p.withdraw)
      }
    }

  val playerResultWrites: OWrites[Player.Result] = OWrites[Player.Result] {
    case Player.Result(player, user, rank) =>
      Json
        .obj(
          "rank"     -> rank,
          "score"    -> player.score,
          "rating"   -> player.rating,
          "username" -> user.name
        )
        .add("title" -> user.title)
        .add("performance" -> player.performanceOption)
        .add("team" -> player.team)
  }

  def playerJson(
      lightUserApi: LightUserApi,
      sheets: Map[String, arena.Sheet],
      streakable: Boolean
  )(rankedPlayer: RankedPlayer)(implicit ec: ExecutionContext): Fu[JsObject] =
    playerJson(lightUserApi, sheets get rankedPlayer.player.userId, rankedPlayer, streakable)

  private[tournament] def playerJson(
      lightUserApi: LightUserApi,
      sheet: Option[arena.Sheet],
      rankedPlayer: RankedPlayer,
      streakable: Boolean
  )(implicit ec: ExecutionContext): Fu[JsObject] = {
    val p = rankedPlayer.player
    lightUserApi async p.userId map { light =>
      Json
        .obj(
          "name"   -> light.fold(p.userId)(_.name),
          "rank"   -> rankedPlayer.rank,
          "rating" -> p.rating,
          "score"  -> p.score,
          "sheet"  -> sheet.map(sheetJson(streakable))
        )
        .add("title" -> light.flatMap(_.title))
        .add("provisional" -> p.provisional)
        .add("withdraw" -> p.withdraw)
        .add("team" -> p.team)
    }
  }

  private[tournament] def sheetJson(streakable: Boolean)(s: arena.Sheet) =
    Json
      .obj(
        "scores" -> s.scores.reverse.map(sheetScoreJson),
        "total"  -> s.total
      )
      .add("fire" -> (streakable && s.onFire))

  private[tournament] def sheetScoreJson(score: arena.Sheet.Score) =
    if (score.flag == arena.Sheet.Normal) JsNumber(score.value)
    else Json.arr(score.value, score.flag.id)

  private[tournament] def tablePlayerJson(
      lightUserApi: LightUserApi,
      player: Player
  )(implicit ec: ExecutionContext): Fu[JsObject] =
    lightUserApi async player.userId map { light =>
      Json
        .obj(
          "id"     -> player.userId,
          "name"   -> light.fold(player.userId)(_.name),
          "order"  -> ~player.order,
          "rating" -> player.rating,
          "score"  -> player.score
        )
        .add("title" -> light.flatMap(_.title))
        .add("provisional" -> player.provisional)
        .add("withdraw" -> player.withdraw)
    }

  private[tournament] def arrangement(a: Arrangement): JsObject =
    Json
      .obj(
        "user1" -> arrangementUser(a.user1),
        "user2" -> arrangementUser(a.user2)
      )
      .add("order", a.order.some.filter(_ > 0))
      .add("name", a.name)
      .add("color", a.color.map(_.name))
      .add("points", a.points.map(arrangementPoints))
      .add("gameId", a.gameId)
      .add("status", a.status.map(_.id))
      .add("winner", a.winner)
      .add("plies", a.plies)
      .add("scheduledAt", a.scheduledAt)
      .add("history", a.history.list.some.filter(_.nonEmpty))

  private def arrangementUser(u: Arrangement.User): JsObject =
    Json
      .obj(
        "id" -> u.id
      )
      .add("readyAt", u.readyAt)
      .add("scheduledAt", u.scheduledAt)

  private def formatDate(date: DateTime) = ISODateTimeFormat.dateTime print date

  private def arrangementPoints(pts: Arrangement.Points): JsObject =
    Json
      .obj(
        "w" -> pts.win,
        "d" -> pts.draw,
        "l" -> pts.lose
      )

  private[tournament] def scheduleJson(s: Schedule) =
    Json.obj(
      "freq"  -> s.freq.name,
      "speed" -> s.speed.key
    )

  implicit val clockWrites: OWrites[TimeControl] = OWrites {
    case TimeControl.RealTime(c) =>
      Json.obj(
        "limit"     -> c.limitSeconds,
        "increment" -> c.incrementSeconds,
        "byoyomi"   -> c.byoyomiSeconds
      )
    case TimeControl.Correspondence(d) =>
      Json.obj(
        "days" -> d
      )
  }

  private[tournament] def positionJson(sfen: Sfen, variant: shogi.variant.Variant): JsObject =
    Thematic.bySfen(sfen, variant) match {
      case Some(pos) =>
        Json
          .obj(
            "japanese" -> pos.japanese,
            "english"  -> pos.english,
            "sfen"     -> pos.sfen
          )
      case None =>
        Json
          .obj(
            "name" -> "Custom position",
            "sfen" -> sfen
          )
    }

  implicit private[tournament] val spotlightWrites: OWrites[Spotlight] = OWrites { s =>
    Json
      .obj(
        "headline"    -> s.headline,
        "description" -> s.description
      )
      .add("iconImg" -> s.iconImg)
      .add("iconFont" -> s.iconFont)
  }

  implicit private[tournament] def perfTypeWrites(implicit lang: Lang): OWrites[PerfType] =
    OWrites { pt =>
      Json.obj(
        "icon" -> pt.iconChar.toString,
        "name" -> pt.trans
      )
    }

  implicit private[tournament] val statsWrites: Writes[TournamentStats] = Json.writes[TournamentStats]
}
