package lila.tournament

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import play.api.i18n.Lang
import play.api.libs.json._
import scala.concurrent.duration._

import lila.common.LightUser
import lila.game.{ GameRepo, LightPov, Game }
import lila.quote.Quote.quoteWriter
import lila.rating.PerfType
import lila.user.User

final class JsonView(
    lightUserApi: lila.user.LightUserApi,
    cached: Cached,
    statsApi: TournamentStatsApi,
    shieldApi: TournamentShieldApi,
    asyncCache: lila.memo.AsyncCache.Builder,
    verify: Condition.Verify,
    duelStore: DuelStore,
    pause: Pause,
    startedSinceSeconds: Int => Boolean
) {

  import JsonView._

  private case class CachableData(
      duels: JsArray,
      featured: Option[JsObject],
      podium: Option[JsArray],
      next: Option[JsObject]
  )

  def apply(
    tour: Tournament,
    page: Option[Int],
    me: Option[User],
    playerInfoExt: Option[PlayerInfoExt],
    socketVersion: Option[Int],
    lang: Lang
  ): Fu[JsObject] = for {
    data <- cachableData get tour.id
    myInfo <- me ?? { u => PlayerRepo.playerInfo(tour.id, u.id) }
    pauseDelay = me flatMap { u => pause.remainingDelay(u.id, tour) }
    stand <- (myInfo, page) match {
      case (_, Some(p)) => standing(tour, p)
      case (Some(i), _) => standing(tour, i.page)
      case _ => standing(tour, 1)
    }
    playerInfoJson <- playerInfoExt ?? { pie =>
      playerInfo(pie).map(_.some)
    }
    verdicts <- me match {
      case None => fuccess(tour.conditions.accepted)
      case Some(user) if myInfo.isDefined => fuccess(tour.conditions.accepted)
      case Some(user) => verify(tour.conditions, user)
    }
    stats <- statsApi(tour)
    myGameId <- me.ifTrue(myInfo.isDefined) ?? { fetchCurrentGameId(tour, _) }
    shieldOwner <- shieldApi currentOwner tour
  } yield Json.obj(
    "id" -> tour.id,
    "createdBy" -> tour.createdBy,
    "system" -> tour.system.toString.toLowerCase,
    "fullName" -> tour.fullName,
    "perf" -> tour.perfType,
    "nbPlayers" -> tour.nbPlayers,
    "minutes" -> tour.minutes,
    "clock" -> tour.clock,
    "verdicts" -> Condition.JSONHandlers.verdictsFor(verdicts, lang),
    "variant" -> tour.variant.key,
    "isStarted" -> tour.isStarted,
    "isFinished" -> tour.isFinished,
    "startsAt" -> formatDate(tour.startsAt),
    "duels" -> data.duels,
    "standing" -> stand,
    "socketVersion" -> socketVersion
  ).add("greatPlayer" -> GreatPlayer.wikiUrl(tour.name).map { url =>
      Json.obj("name" -> tour.name, "url" -> url)
    }).add("position" -> tour.position.some.filterNot(_.initial).map(positionJson))
    .add("schedule" -> tour.schedule.map(scheduleJson))
    .add("private" -> tour.`private`)
    .add("isRecentlyFinished" -> tour.isRecentlyFinished)
    .add("secondsToFinish" -> tour.isStarted.option(tour.secondsToFinish))
    .add("secondsToStart" -> tour.isCreated.option(tour.secondsToStart))
    .add("me" -> myInfo.map(myInfoJson(me, pauseDelay)))
    .add("featured" -> data.featured)
    .add("podium" -> data.podium)
    .add("playerInfo" -> playerInfoJson)
    .add("quote" -> tour.isCreated.option(lila.quote.Quote.one(tour.id)))
    .add("spotlight" -> tour.spotlight)
    .add("pairingsClosed" -> tour.pairingsClosed)
    .add("stats" -> stats)
    .add("next" -> data.next)
    .add("myGameId" -> myGameId)
    .add("defender" -> shieldOwner.map(_.value))

  def standing(tour: Tournament, page: Int): Fu[JsObject] =
    if (page == 1) firstPageCache get tour.id
    else computeStanding(tour, page)

  def clearCache(id: String): Unit = {
    firstPageCache invalidate id
    cachableData invalidate id
  }

  def playerInfo(info: PlayerInfoExt): Fu[JsObject] = for {
    ranking <- cached ranking info.tour
    sheet <- cached.sheet(info.tour, info.user.id)
  } yield info match {
    case PlayerInfoExt(tour, user, player, povs) =>
      val isPlaying = povs.headOption.??(_.game.playable)
      val povScores: List[(LightPov, Option[Score])] = povs zip {
        (isPlaying ?? List(none[Score])) ::: sheet.scores.map(some)
      }
      Json.obj(
        "player" -> Json.obj(
          "id" -> user.id,
          "name" -> user.username,
          "rating" -> player.rating,
          "ratingDiff" -> 0, // # temp mobile app BC - remove me #TODO
          "score" -> player.score,
          "fire" -> player.fire,
          "nb" -> sheetNbs(user.id, sheet)
        ).add("title" -> user.title)
          .add("performance" -> player.performanceOption)
          .add("rank" -> ranking.get(user.id).map(1+))
          .add("provisional" -> player.provisional)
          .add("withdraw" -> player.withdraw),
        "pairings" -> povScores.map {
          case (pov, score) => Json.obj(
            "id" -> pov.gameId,
            "color" -> pov.color.name,
            "op" -> gameUserJson(pov.opponent.userId, pov.opponent.rating),
            "win" -> pov.win,
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
        GameRepo game pairing.gameId flatMap {
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

  private def computeStanding(tour: Tournament, page: Int): Fu[JsObject] = for {
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

  private val firstPageCache = asyncCache.clearable[String, JsObject](
    name = "tournament.firstPage",
    id => TournamentRepo byId id flatten s"No such tournament: $id" flatMap { computeStanding(_, 1) },
    expireAfter = _.ExpireAfterWrite(1 second)
  )

  private val cachableData = asyncCache.clearable[String, CachableData](
    name = "tournament.json.cachable",
    id => for {
      tour <- TournamentRepo byId id
      duels = duelStore.bestRated(id, 6)
      jsonDuels <- duels.map(duelJson).sequenceFu
      featured <- tour ?? fetchFeaturedGame
      podium <- tour.exists(_.isFinished) ?? podiumJsonCache.get(id)
      next <- tour.filter(_.isFinished) ?? cached.findNext map2 nextJson
    } yield CachableData(
      duels = JsArray(jsonDuels),
      featured = featured map featuredJson,
      podium = podium,
      next = next
    ),
    expireAfter = _.ExpireAfterWrite(1 second)
  )

  private def nextJson(tour: Tournament) = Json.obj(
    "id" -> tour.id,
    "name" -> tour.fullName,
    "perf" -> tour.perfType,
    "nbPlayers" -> tour.nbPlayers,
    "finishesAt" -> tour.isStarted.option(tour.finishesAt).map(formatDate),
    "startsAt" -> tour.isCreated.option(tour.startsAt).map(formatDate)
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

  private def myInfoJson(u: Option[User], delay: Option[Pause.Delay])(i: PlayerInfo) = Json.obj(
    "rank" -> i.rank,
    "withdraw" -> i.withdraw,
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
        "ratingDiff" -> 0, // # temp mobile app BC - remove me #TODO
        "score" -> p.score,
        "sheet" -> sheet.map(sheetJson)
      ).add("title" -> light.flatMap(_.title))
        .add("provisional" -> p.provisional)
        .add("withdraw" -> p.withdraw)
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
