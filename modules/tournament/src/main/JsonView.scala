package lila.tournament

import org.joda.time.format.ISODateTimeFormat
import play.api.libs.json._
import scala.concurrent.duration._

import lila.common.LightUser
import lila.common.PimpedJson._
import lila.game.{ Game, GameRepo, Pov }
import lila.quote.Quote.quoteWriter
import lila.user.User

final class JsonView(
    getLightUser: String => Option[LightUser],
    cached: Cached,
    performance: Performance) {

  import JsonView._

  private case class CachableData(
    pairings: JsArray,
    featured: Option[JsObject],
    podium: Option[JsArray])

  def apply(
    tour: Tournament,
    page: Option[Int],
    me: Option[String],
    playerInfoExt: Option[PlayerInfoExt],
    socketVersion: Option[Int]): Fu[JsObject] = for {
    data <- cachableData(tour.id)
    myInfo <- me ?? { PlayerRepo.playerInfo(tour.id, _) }
    stand <- (myInfo, page) match {
      case (_, Some(p)) => standing(tour, p)
      case (Some(i), _) => standing(tour, i.page)
      case _            => standing(tour, 1)
    }
    playerInfoJson <- playerInfoExt ?? { pie =>
      playerInfo(pie).map(_.some)
    }
  } yield Json.obj(
    "id" -> tour.id,
    "createdBy" -> tour.createdBy,
    "system" -> tour.system.toString.toLowerCase,
    "fullName" -> tour.fullName,
    "greatPlayer" -> GreatPlayer.wikiUrl(tour.name).map { url =>
      Json.obj("name" -> tour.name, "url" -> url)
    },
    "nbPlayers" -> tour.nbPlayers,
    "minutes" -> tour.minutes,
    "clock" -> clockJson(tour.clock),
    "position" -> tour.position.some.filterNot(_.initial).map(positionJson),
    "private" -> tour.`private`.option(true),
    "variant" -> tour.variant.key,
    "isStarted" -> tour.isStarted,
    "isFinished" -> tour.isFinished,
    "isRecentlyFinished" -> tour.isRecentlyFinished.option(true),
    "schedule" -> tour.schedule.map(scheduleJson),
    "secondsToFinish" -> tour.isStarted.option(tour.secondsToFinish),
    "secondsToStart" -> tour.isCreated.option(tour.secondsToStart),
    "startsAt" -> tour.isCreated.option(ISODateTimeFormat.dateTime.print(tour.startsAt)),
    "pairings" -> data.pairings,
    "standing" -> stand,
    "me" -> myInfo.map(myInfoJson),
    "featured" -> data.featured,
    "podium" -> data.podium,
    "playerInfo" -> playerInfoJson,
    "quote" -> tour.isCreated.option(lila.quote.Quote.one(tour.id)),
    "socketVersion" -> socketVersion
  ).noNull

  def standing(tour: Tournament, page: Int): Fu[JsObject] =
    if (page == 1) firstPageCache(tour.id)
    else computeStanding(tour, page)

  def clearCache(id: String) =
    firstPageCache.remove(id) >> cachableData.remove(id)

  def playerInfo(info: PlayerInfoExt): Fu[JsObject] = for {
    ranking <- cached ranking info.tour
    pairings <- PairingRepo.finishedByPlayerChronological(info.tour.id, info.user.id)
    sheet = info.tour.system.scoringSystem.sheet(info.tour, info.user.id, pairings)
    tpr <- performance(info.tour, info.player, pairings)
  } yield info match {
    case PlayerInfoExt(tour, user, player, povs) => Json.obj(
      "player" -> Json.obj(
        "id" -> user.id,
        "name" -> user.username,
        "title" -> user.title,
        "rank" -> ranking.get(user.id).map(1+),
        "rating" -> player.rating,
        "provisional" -> player.provisional.option(true),
        "withdraw" -> player.withdraw.option(true),
        "score" -> player.score,
        "ratingDiff" -> player.ratingDiff,
        "fire" -> player.fire,
        "nb" -> sheetNbs(user.id, sheet, pairings),
        "performance" -> tpr
      ).noNull,
      "pairings" -> povs.map { pov =>
        Json.obj(
          "id" -> pov.gameId,
          "color" -> pov.color.name,
          "op" -> gameUserJson(pov.opponent.userId, pov.opponent.rating),
          "win" -> pov.win,
          "status" -> pov.game.status.id,
          "berserk" -> pov.player.berserk.option(true)
        ).noNull
      }
    )
  }

  private def fetchFeaturedGame(tour: Tournament): Fu[Option[FeaturedGame]] =
    tour.featuredId.ifTrue(tour.isStarted) ?? PairingRepo.byId flatMap {
      _ ?? { pairing =>
        GameRepo game pairing.gameId flatMap {
          _ ?? { game =>
            cached ranking tour flatMap { ranking =>
              PlayerRepo.pairByTourAndUserIds(tour.id, pairing.user1, pairing.user2) map { pairOption =>
                for {
                  players <- pairOption
                  (p1, p2) = players
                  rp1 <- RankedPlayer(ranking)(p1)
                  rp2 <- RankedPlayer(ranking)(p2)
                } yield FeaturedGame(game, rp1, rp2)
              }
            }
          }
        }
      }
    }

  private def sheetNbs(userId: String, sheet: ScoreSheet, pairings: Pairings) = sheet match {
    case s: arena.ScoringSystem.Sheet => Json.obj(
      "game" -> s.scores.size,
      "berserk" -> pairings.foldLeft(0) {
        case (nb, p) => nb + p.berserkOf(userId)
      },
      "win" -> s.scores.count(_.isWin))
  }

  private def computeStanding(tour: Tournament, page: Int): Fu[JsObject] = for {
    rankedPlayers <- PlayerRepo.bestByTourWithRankByPage(tour.id, 10, page max 1)
    sheets <- rankedPlayers.map { p =>
      PairingRepo.finishedByPlayerChronological(tour.id, p.player.userId) map { pairings =>
        p.player.userId -> tour.system.scoringSystem.sheet(tour, p.player.userId, pairings)
      }
    }.sequenceFu.map(_.toMap)
  } yield Json.obj(
    "page" -> page,
    "players" -> rankedPlayers.map(playerJson(sheets, tour))
  )

  private val firstPageCache = lila.memo.AsyncCache[String, JsObject](
    (id: String) => TournamentRepo byId id flatten s"No such tournament: $id" flatMap { computeStanding(_, 1) },
    timeToLive = 1 second)

  private val cachableData = lila.memo.AsyncCache[String, CachableData](id =>
    for {
      pairings <- PairingRepo.recentByTour(id, 40)
      tour <- TournamentRepo byId id
      featured <- tour ?? fetchFeaturedGame
      podium <- podiumJson(id)
    } yield CachableData(
      JsArray(pairings map pairingJson),
      featured map featuredJson,
      podium),
    timeToLive = 1 second)

  private def featuredJson(featured: FeaturedGame) = {
    val game = featured.game
    def playerJson(rp: RankedPlayer, p: lila.game.Player) = {
      val light = getLightUser(rp.player.userId)
      Json.obj(
        "rank" -> rp.rank,
        "name" -> light.fold(rp.player.userId)(_.name),
        "title" -> light.flatMap(_.title),
        "rating" -> rp.player.rating,
        "ratingDiff" -> rp.player.ratingDiff,
        "berserk" -> p.berserk.option(true)
      ).noNull
    }
    Json.obj(
      "id" -> game.id,
      "fen" -> (chess.format.Forsyth exportBoard game.toChess.board),
      "color" -> (game.variant match {
        case chess.variant.RacingKings => chess.White
        case _                         => game.firstColor
      }).name,
      "lastMove" -> ~game.castleLastMoveTime.lastMoveString,
      "white" -> playerJson(featured.white, game player chess.White),
      "black" -> playerJson(featured.black, game player chess.Black))
  }

  private def myInfoJson(i: PlayerInfo) = Json.obj(
    "rank" -> i.rank,
    "withdraw" -> i.withdraw)

  private def gameUserJson(player: lila.game.Player): JsObject =
    gameUserJson(player.userId, player.rating)

  private def gameUserJson(userId: Option[String], rating: Option[Int]): JsObject = {
    val light = userId flatMap getLightUser
    Json.obj(
      "name" -> light.map(_.name),
      "title" -> light.flatMap(_.title),
      "rating" -> rating
    ).noNull
  }

  private def sheetJson(sheet: ScoreSheet) = sheet match {
    case s: arena.ScoringSystem.Sheet =>
      val o = Json.obj(
        "scores" -> s.scores.reverse.map { score =>
          if (score.flag == arena.ScoringSystem.Normal) JsNumber(score.value)
          else Json.arr(score.value, score.flag.id)
        },
        "total" -> s.total)
      s.onFire.fold(o + ("fire" -> JsBoolean(true)), o)
  }

  private def playerJson(sheets: Map[String, ScoreSheet], tour: Tournament)(rankedPlayer: RankedPlayer): JsObject =
    playerJson(sheets get rankedPlayer.player.userId, tour, rankedPlayer)

  private def playerJson(sheet: Option[ScoreSheet], tour: Tournament, rankedPlayer: RankedPlayer): JsObject = {
    val p = rankedPlayer.player
    val light = getLightUser(p.userId)
    Json.obj(
      "rank" -> rankedPlayer.rank,
      "name" -> light.fold(p.userId)(_.name),
      "title" -> light.flatMap(_.title),
      "rating" -> p.rating,
      "provisional" -> p.provisional.option(true),
      "withdraw" -> p.withdraw.option(true),
      "score" -> p.score,
      "ratingDiff" -> p.ratingDiff,
      "sheet" -> sheet.map(sheetJson)
    ).noNull
  }

  private def podiumJson(id: String): Fu[Option[JsArray]] =
    TournamentRepo finishedById id flatMap {
      _ ?? { tour =>
        PlayerRepo.bestByTourWithRank(id, 3).flatMap {
          _.map {
            case rp@RankedPlayer(_, player) => for {
              pairings <- PairingRepo.finishedByPlayerChronological(tour.id, player.userId)
              sheet = tour.system.scoringSystem.sheet(tour, player.userId, pairings)
              tpr <- performance(tour, player, pairings)
            } yield playerJson(sheet.some, tour, rp) ++ Json.obj(
              "nb" -> sheetNbs(player.userId, sheet, pairings),
              "performance" -> tpr)
          }.sequenceFu
        } map { l => JsArray(l).some }
      }
    }

  private def pairingUserJson(userId: String) = getLightUser(userId).fold(userId)(_.name)

  private def pairingJson(p: Pairing) = Json.obj(
    "id" -> p.gameId,
    "u" -> Json.arr(pairingUserJson(p.user1), pairingUserJson(p.user2)),
    "s" -> (if (p.finished) p.winner match {
      case Some(w) if w == p.user1 => 2
      case Some(w)                 => 3
      case _                       => 1
    }
    else 0))
}

object JsonView {

  private[tournament] def scheduleJson(s: Schedule) = Json.obj(
    "freq" -> s.freq.name,
    "speed" -> s.speed.name)

  private[tournament] def clockJson(c: TournamentClock) = Json.obj(
    "limit" -> c.limit,
    "increment" -> c.increment)

  private[tournament] def positionJson(s: chess.StartingPosition) = Json.obj(
    "eco" -> s.eco,
    "name" -> s.name,
    "fen" -> s.fen)
}
