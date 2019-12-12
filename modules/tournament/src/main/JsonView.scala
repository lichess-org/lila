package lila.tournament

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import play.api.libs.json._
import scala.concurrent.duration._

import lila.common.{ Lang, LightUser }
import lila.game.{ LightPov, Game }
import lila.hub.lightTeam._
import lila.quote.Quote.quoteWriter
import lila.rating.PerfType
import lila.socket.Socket.SocketVersion
import lila.user.User

final class JsonView(
    lightUserApi: lila.user.LightUserApi,
    cached: Cached,
    statsApi: TournamentStatsApi,
    shieldApi: TournamentShieldApi,
    asyncCache: lila.memo.AsyncCache.Builder,
    proxyGame: Game.ID => Fu[Option[Game]],
    verify: Condition.Verify,
    duelStore: DuelStore,
    pause: Pause,
    startedSinceSeconds: Int => Boolean
) {

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
    getUserTeamIds: User => Fu[List[TeamId]],
    getTeamName: TeamId => Option[String],
    playerInfoExt: Option[PlayerInfoExt],
    socketVersion: Option[SocketVersion],
    partial: Boolean,
    lang: Lang
  ): Fu[JsObject] = for {
    data <- cachableData get tour.id
    myInfo <- me ?? { myInfo(tour, _) }
    pauseDelay = me flatMap { u => pause.remainingDelay(u.id, tour) }
    full = !partial
    stand <- (myInfo, page) match {
      case (_, Some(p)) => standing(tour, p)
      case (Some(i), _) => standing(tour, i.page)
      case _ => standing(tour, 1)
    }
    playerInfoJson <- playerInfoExt ?? { pie =>
      playerInfoExtended(tour, pie).map(_.some)
    }
    verdicts <- full ?? {
      me match {
        case None => fuccess(tour.conditions.accepted.some)
        case Some(user) if myInfo.isDefined => fuccess(tour.conditions.accepted.some)
        case Some(user) => verify(tour.conditions, user, getUserTeamIds) map some
      }
    }
    stats <- statsApi(tour)
    shieldOwner <- full.?? { shieldApi currentOwner tour }
    teamsToJoinWith <- ~(for {
      u <- me; battle <- tour.teamBattle; if full
    } yield getUserTeamIds(u) map { teams => battle.teams intersect teams.toSet })
    teamStanding <- getTeamStanding(tour)
  } yield Json.obj(
    "nbPlayers" -> tour.nbPlayers,
    "duels" -> data.duels,
    "standing" -> stand
  )
    .add("isStarted" -> tour.isStarted)
    .add("isFinished" -> tour.isFinished)
    .add("isRecentlyFinished" -> tour.isRecentlyFinished)
    .add("secondsToFinish" -> tour.isStarted.option(tour.secondsToFinish))
    .add("secondsToStart" -> tour.isCreated.option(tour.secondsToStart))
    .add("me" -> myInfo.map(myInfoJson(me, pauseDelay)))
    .add("featured" -> data.featured)
    .add("podium" -> data.podium)
    .add("playerInfo" -> playerInfoJson)
    .add("pairingsClosed" -> tour.pairingsClosed)
    .add("stats" -> stats)
    .add("socketVersion" -> socketVersion.map(_.value))
    .add("teamStanding" -> teamStanding)
    .add("duelTeams" -> data.duelTeams) ++
    full.?? {
      Json.obj(
        "id" -> tour.id,
        "createdBy" -> tour.createdBy,
        "startsAt" -> formatDate(tour.startsAt),
        "system" -> tour.system.toString.toLowerCase,
        "fullName" -> tour.fullName,
        "minutes" -> tour.minutes,
        "perf" -> full.option(tour.perfType),
        "clock" -> full.option(tour.clock),
        "variant" -> full.option(tour.variant.key)
      ).add("spotlight" -> tour.spotlight)
        .add("berserkable" -> tour.berserkable)
        .add("position" -> full.option(tour.position).filterNot(_.initial).map(positionJson))
        .add("verdicts" -> verdicts.map(Condition.JSONHandlers.verdictsFor(_, lang)))
        .add("schedule" -> tour.schedule.map(scheduleJson))
        .add("private" -> tour.isPrivate)
        .add("quote" -> tour.isCreated.option(lila.quote.Quote.one(tour.id)))
        .add("defender" -> shieldOwner.map(_.value))
        .add("greatPlayer" -> GreatPlayer.wikiUrl(tour.name).map { url =>
          Json.obj("name" -> tour.name, "url" -> url)
        })
        .add("teamBattle" -> tour.teamBattle.map { battle =>
          Json.obj(
            "teams" -> JsObject(battle.sortedTeamIds.map { id =>
              id -> JsString(getTeamName(id).getOrElse(id))
            })
          ).add("joinWith" -> me.isDefined.option(teamsToJoinWith.toList.sorted))
        })
    }

  def clearCache(tour: Tournament): Unit = {
    standing clearCache tour
    cachableData invalidate tour.id
  }

  def myInfo(tour: Tournament, me: User): Fu[Option[MyInfo]] =
    PlayerRepo.find(tour.id, me.id) flatMap {
      _ ?? { player =>
        fetchCurrentGameId(tour, me) flatMap { gameId =>
          getOrGuessRank(tour, player) map { rank =>
            MyInfo(rank + 1, player.withdraw, gameId).some
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
        case None => PlayerRepo.computeRankOf(player)
      }
    }

  def playerInfoExtended(tour: Tournament, info: PlayerInfoExt): Fu[JsObject] = for {
    ranking <- cached ranking tour
    sheet <- cached.sheet(tour, info.user.id)
  } yield info match {
    case PlayerInfoExt(user, player, povs) =>
      val isPlaying = povs.headOption.??(_.game.playable)
      val povScores: List[(LightPov, Option[Score])] = povs zip {
        (isPlaying ?? List(none[Score])) ::: sheet.scores.map(some)
      }
      Json.obj(
        "player" -> Json.obj(
          "id" -> user.id,
          "name" -> user.username,
          "rating" -> player.rating,
          "score" -> player.score,
          "fire" -> player.fire,
          "nb" -> sheetNbs(user.id, sheet)
        ).add("title" -> user.title)
          .add("performance" -> player.performanceOption)
          .add("rank" -> ranking.get(user.id).map(1+))
          .add("provisional" -> player.provisional)
          .add("withdraw" -> player.withdraw)
          .add("team" -> player.team),
        "pairings" -> povScores.map {
          case (pov, score) => Json.obj(
            "id" -> pov.gameId,
            "color" -> pov.color.name,
            "op" -> gameUserJson(pov.opponent.userId, pov.opponent.rating),
            "win" -> score.flatMap(_.isWin),
            "status" -> pov.game.status.id,
            "score" -> score.map(sheetScoreJson)
          ).add("berserk" -> pov.player.berserk)
        }
      )
  }

  private def fetchCurrentGameId(tour: Tournament, user: User): Fu[Option[Game.ID]] =
    if (startedSinceSeconds(60)) fuccess(duelStore.find(tour, user))
    else PairingRepo.playingByTourAndUserId(tour.id, user.id)

  private def fetchFeaturedGame(tour: Tournament): Fu[Option[FeaturedGame]] =
    tour.featuredId.ifTrue(tour.isStarted) ?? PairingRepo.byId flatMap {
      _ ?? { pairing =>
        proxyGame(pairing.gameId) flatMap {
          _ ?? { game =>
            cached ranking tour flatMap { ranking =>
              PlayerRepo.pairByTourAndUserIds(tour.id, pairing.user1, pairing.user2) map { pairOption =>
                for {
                  (p1, p2) <- pairOption
                  rp1 <- RankedPlayer(ranking)(p1)
                  rp2 <- RankedPlayer(ranking)(p2)
                } yield FeaturedGame(game, rp1, rp2)
              }
            }
          }
        }
      }
    }

  private def sheetNbs(userId: String, sheet: ScoreSheet) = sheet match {
    case s: arena.ScoringSystem.Sheet => Json.obj(
      "game" -> s.scores.size,
      "berserk" -> s.scores.count(_.isBerserk),
      "win" -> s.scores.count(_.res == arena.ScoringSystem.ResWin)
    )
  }

  object standing {

    def apply(tour: Tournament, page: Int): Fu[JsObject] =
      if (page == 1) first get tour.id
      else if (page > 50 && tour.isCreated) deep.get(tour.id -> page)
      else compute(tour, page)

    val first = asyncCache.clearable[Tournament.ID, JsObject](
      name = "tournament.page.first",
      id => compute(id, 1),
      expireAfter = _.ExpireAfterWrite(1 second)
    )
    val deep = asyncCache.clearable[(Tournament.ID, Int), JsObject](
      name = "tournament.page.deep",
      t => compute(t._1, t._2),
      expireAfter = _.ExpireAfterWrite(10 second)
    )

    private[JsonView] def clearCache(tour: Tournament): Unit = {
      first invalidate tour.id
      // no need to invalidate, these are only cached when tour.isCreated
      // if (tour.nbPlayers > 500) (51 to math.ceil(tour.nbPlayers / 10d).toInt) foreach { page =>
      //   deep.invalidate(tour.id -> page)
      // }
    }

    private def compute(id: Tournament.ID, page: Int): Fu[JsObject] =
      TournamentRepo byId id flatten s"No such tournament: $id" flatMap { compute(_, page) }

    private def compute(tour: Tournament, page: Int): Fu[JsObject] = for {
      rankedPlayers <- PlayerRepo.bestByTourWithRankByPage(tour.id, 10, page max 1)
      sheets <- rankedPlayers.map { p =>
        cached.sheet(tour, p.player.userId) map { sheet =>
          p.player.userId -> sheet
        }
      }.sequenceFu.map(_.toMap)
      players <- rankedPlayers.map(playerJson(sheets, tour)).sequenceFu
    } yield Json.obj(
      "page" -> page,
      "players" -> players
    )
  }

  private val cachableData = asyncCache.clearable[String, CachableData](
    name = "tournament.json.cachable",
    id => for {
      tour <- TournamentRepo byId id
      duels = duelStore.bestRated(id, 6)
      jsonDuels <- duels.map(duelJson).sequenceFu
      duelTeams <- tour.flatMap(_.teamBattle).?? { battle =>
        PlayerRepo.teamsOfPlayers(id, duels.foldLeft(List.empty[User.ID])(_ ::: _.userIds)) map { teams =>
          JsObject(teams map {
            case (userId, teamId) => (userId, JsString(teamId))
          }).some
        }
      }
      featured <- tour ?? fetchFeaturedGame
      podium <- tour.exists(_.isFinished) ?? podiumJsonCache.get(id)
    } yield CachableData(
      duels = JsArray(jsonDuels),
      duelTeams = duelTeams,
      featured = featured map featuredJson,
      podium = podium
    ),
    expireAfter = _.ExpireAfterWrite(1 second)
  )

  private def featuredJson(featured: FeaturedGame) = {
    val game = featured.game
    def ofPlayer(rp: RankedPlayer, p: lila.game.Player) = {
      val light = lightUserApi sync rp.player.userId
      Json.obj(
        "rank" -> rp.rank,
        "name" -> light.fold(rp.player.userId)(_.name),
        "rating" -> rp.player.rating
      ).add("title" -> light.flatMap(_.title))
        .add("berserk" -> p.berserk)
    }
    Json.obj(
      "id" -> game.id,
      "fen" -> (chess.format.Forsyth exportBoard game.board),
      "color" -> (game.variant match {
        case chess.variant.RacingKings => chess.White
        case _ => game.firstColor
      }).name,
      "lastMove" -> ~game.lastMoveKeys,
      "white" -> ofPlayer(featured.white, game player chess.White),
      "black" -> ofPlayer(featured.black, game player chess.Black)
    )
  }

  private def myInfoJson(u: Option[User], delay: Option[Pause.Delay])(i: MyInfo) = Json.obj(
    "rank" -> i.rank,
    "withdraw" -> i.withdraw,
    "gameId" -> i.gameId,
    "username" -> u.map(_.titleUsername)
  ).add("pauseDelay", delay.map(_.seconds))

  private def gameUserJson(userId: Option[String], rating: Option[Int]): JsObject = {
    val light = userId flatMap lightUserApi.sync
    Json.obj("rating" -> rating)
      .add("name" -> light.map(_.name))
      .add("title" -> light.flatMap(_.title))
  }

  private def sheetJson(sheet: ScoreSheet) = sheet match {
    case s: arena.ScoringSystem.Sheet => Json.obj(
      "scores" -> s.scores.reverse.map(arenaSheetScoreJson),
      "total" -> s.total
    ).add("fire" -> s.onFire)
  }

  private def arenaSheetScoreJson(score: arena.ScoringSystem.Score) =
    if (score.flag == arena.ScoringSystem.Normal) JsNumber(score.value)
    else Json.arr(score.value, score.flag.id)

  private def sheetScoreJson(score: Score) = score match {
    case s: arena.ScoringSystem.Score => arenaSheetScoreJson(s)
    case s => JsNumber(score.value)
  }

  private def playerJson(sheets: Map[String, ScoreSheet], tour: Tournament)(rankedPlayer: RankedPlayer): Fu[JsObject] =
    playerJson(sheets get rankedPlayer.player.userId, tour, rankedPlayer)

  private def playerJson(sheet: Option[ScoreSheet], tour: Tournament, rankedPlayer: RankedPlayer): Fu[JsObject] = {
    val p = rankedPlayer.player
    lightUserApi async p.userId map { light =>
      Json.obj(
        "name" -> light.fold(p.userId)(_.name),
        "rank" -> rankedPlayer.rank,
        "rating" -> p.rating,
        "score" -> p.score,
        "sheet" -> sheet.map(sheetJson)
      ).add("title" -> light.flatMap(_.title))
        .add("provisional" -> p.provisional)
        .add("withdraw" -> p.withdraw)
        .add("team" -> p.team)
    }
  }

  private val podiumJsonCache = asyncCache.multi[Tournament.ID, Option[JsArray]](
    name = "tournament.podiumJson",
    id => TournamentRepo finishedById id flatMap {
      _ ?? { tour =>
        PlayerRepo.bestByTourWithRank(id, 3).flatMap {
          _.map {
            case rp @ RankedPlayer(_, player) => for {
              sheet <- cached.sheet(tour, player.userId)
              json <- playerJson(sheet.some, tour, rp)
            } yield json ++ Json.obj(
              "nb" -> sheetNbs(player.userId, sheet)
            ).add("performance" -> player.performanceOption)
          }.sequenceFu
        } map { l => JsArray(l).some }
      }
    },
    expireAfter = _.ExpireAfterWrite(10 seconds)
  )

  private def duelPlayerJson(p: Duel.DuelPlayer): Fu[JsObject] =
    lightUserApi.async(p.name.id) map { u =>
      Json.obj(
        "n" -> u.fold(p.name.value)(_.name),
        "r" -> p.rating.value,
        "k" -> p.rank.value
      ).add("t" -> u.flatMap(_.title))
    }

  private def duelJson(d: Duel): Fu[JsObject] = for {
    u1 <- duelPlayerJson(d.p1)
    u2 <- duelPlayerJson(d.p2)
  } yield Json.obj(
    "id" -> d.gameId,
    "p" -> Json.arr(u1, u2)
  )

  private val teamStandingCache = asyncCache.clearable[Tournament.ID, JsArray](
    name = "tournament.teamStanding",
    id => TournamentRepo.teamBattleOf(id) flatMap {
      _.fold(fuccess(JsArray())) { battle =>
        PlayerRepo.bestTeamIdsByTour(id, battle) map { ranked =>
          JsArray(ranked map teamBattleRankedWrites.writes)
        }
      }
    },
    expireAfter = _.ExpireAfterWrite(1 second)
  )

  private def getTeamStanding(tour: Tournament): Fu[Option[JsArray]] =
    tour.isTeamBattle ?? {
      teamStandingCache get tour.id dmap some
    }

  private implicit val teamBattleRankedWrites: Writes[TeamBattle.RankedTeam] = OWrites { rt =>
    Json.obj(
      "rank" -> rt.rank,
      "id" -> rt.teamId,
      "score" -> rt.score,
      "players" -> rt.leaders.map { p =>
        Json.obj(
          "user" -> lightUserApi.sync(p.userId),
          "score" -> p.score
        )
      }
    )
  }

  private val teamInfoCache = asyncCache.clearable[(Tournament.ID, TeamId), Option[JsObject]](
    name = "tournament.teamInfo",
    f = {
      case (tourId, teamId) => TournamentRepo.teamBattleOf(tourId) flatMap {
        _ ?? { battle =>
          PlayerRepo.teamInfo(tourId, teamId, battle) flatMap { info =>
            lightUserApi.preloadMany(info.topPlayers.map(_.userId)) inject Json.obj(
              "id" -> teamId,
              "nbPlayers" -> info.nbPlayers,
              "rating" -> info.avgRating,
              "perf" -> info.avgPerf,
              "score" -> info.avgScore,
              "topPlayers" -> info.topPlayers.flatMap { p =>
                lightUserApi.sync(p.userId) map { user =>
                  Json.obj(
                    "name" -> user.name,
                    "rating" -> p.rating,
                    "score" -> p.score
                  ).add("fire" -> p.fire)
                    .add("title" -> user.title)
                }
              }
            ).some
          }
        }
      }
    },
    expireAfter = _.ExpireAfterWrite(1 second)
  )

  def teamInfo(tour: Tournament, teamId: TeamId): Fu[Option[JsObject]] =
    tour.isTeamBattle ?? {
      teamInfoCache get (tour.id -> teamId)
    }
}

object JsonView {

  def top(t: TournamentTop, getLightUser: LightUser.GetterSync): JsArray = JsArray {
    t.value.map { p =>
      val light = getLightUser(p.userId)
      Json.obj(
        "n" -> light.fold(p.userId)(_.name),
        "s" -> p.score
      ).add("t" -> light.flatMap(_.title))
        .add("f" -> p.fire)
        .add("w" -> p.withdraw)
    }
  }

  val playerResultWrites: OWrites[Player.Result] = OWrites[Player.Result] {
    case Player.Result(player, user, rank) => Json.obj(
      "rank" -> rank,
      "score" -> player.score,
      "rating" -> player.rating,
      "username" -> user.name
    ).add("title" -> user.title)
      .add("performance" -> player.performanceOption)
  }

  private def formatDate(date: DateTime) = ISODateTimeFormat.dateTime print date

  private[tournament] def scheduleJson(s: Schedule) = Json.obj(
    "freq" -> s.freq.name,
    "speed" -> s.speed.name
  )

  implicit val clockWrites: OWrites[chess.Clock.Config] = OWrites { clock =>
    Json.obj(
      "limit" -> clock.limitSeconds,
      "increment" -> clock.incrementSeconds
    )
  }

  private[tournament] def positionJson(s: chess.StartingPosition) = Json.obj(
    "eco" -> s.eco,
    "name" -> s.name,
    "wikiPath" -> s.wikiPath,
    "fen" -> s.fen
  )

  private[tournament] implicit val spotlightWrites: OWrites[Spotlight] = OWrites { s =>
    Json.obj(
      "headline" -> s.headline,
      "description" -> s.description
    ).add("iconImg" -> s.iconImg)
      .add("iconFont" -> s.iconFont)
  }

  private[tournament] implicit val perfTypeWrites: OWrites[PerfType] = OWrites { pt =>
    Json.obj(
      "icon" -> pt.iconChar.toString,
      "name" -> pt.name
    )
  }

  private[tournament] implicit val statsWrites: Writes[TournamentStats] = Json.writes[TournamentStats]
}
