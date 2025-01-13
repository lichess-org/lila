package lila.relay

import scala.collection.immutable.SeqMap
import play.api.libs.json.*
import scalalib.Json.writeAs
import scalalib.Debouncer
import chess.{ ByColor, Color, FideId, Outcome, PlayerName, IntRating }
import chess.rating.{ Elo, IntRatingDiff }

import lila.db.dsl.*
import lila.study.{ ChapterPreviewApi, StudyPlayer }
import lila.study.StudyPlayer.json.given
import lila.memo.CacheApi
import lila.core.fide.Player as FidePlayer
import lila.common.Json.given
import lila.core.fide.FideTC

// Player in a tournament with current performance rating and list of games
case class RelayPlayer(
    player: StudyPlayer.WithFed,
    score: Option[Float],
    ratingDiff: Option[IntRatingDiff],
    performance: Option[IntRating],
    games: Vector[RelayPlayer.Game]
):
  export player.player.*
  def withGame(game: RelayPlayer.Game) = copy(games = games :+ game)
  def eloGames: Vector[Elo.Game]       = games.flatMap(_.eloGame)

object RelayPlayer:
  case class Game(
      round: RelayRoundId,
      id: StudyChapterId,
      opponent: StudyPlayer.WithFed,
      color: Color,
      points: Option[Outcome.GamePoints]
  ):
    def playerPoints = points.map(_(color))
    // only rate draws and victories, not exotic results
    def isRated = points.exists(_.mapReduce(_.value)(_ + _) == 1)
    def eloGame = for
      pp <- playerPoints
      if isRated
      opRating <- opponent.rating
    yield Elo.Game(pp, opRating.into(Elo))

  object json:
    given Writes[Outcome]            = Json.writes
    given Writes[Outcome.Points]     = writeAs(_.show)
    given Writes[Outcome.GamePoints] = writeAs(points => Outcome.showPoints(points.some))
    given Writes[RelayPlayer.Game]   = Json.writes
    given OWrites[RelayPlayer] = OWrites: p =>
      Json.toJsObject(p.player) ++ Json
        .obj("played" -> p.games.count(_.points.isDefined))
        .add("score" -> p.score)
        .add("ratingDiff" -> p.ratingDiff)
        .add("performance" -> p.performance)
    def full(tour: RelayTour)(p: RelayPlayer, fidePlayer: Option[FidePlayer]): JsObject =
      val tc = tour.info.fideTcOrGuess
      lazy val eloPlayer = p.rating
        .map(_.into(Elo))
        .orElse(fidePlayer.flatMap(_.ratingOf(tc)))
        .map:
          Elo.Player(_, fidePlayer.fold(chess.rating.KFactor.default)(_.kFactorOf(tc)))
      val gamesJson = p.games.map: g =>
        val rd = tour.showRatingDiffs.so:
          (eloPlayer, g.eloGame).tupled.map: (ep, eg) =>
            Elo.computeRatingDiff(ep, List(eg))
        Json
          .obj(
            "round"    -> g.round,
            "id"       -> g.id,
            "opponent" -> g.opponent,
            "color"    -> g.color,
            "points"   -> g.playerPoints
          )
          .add("ratingDiff" -> rd)
      Json.toJsObject(p).add("fide", fidePlayer) ++ Json.obj("games" -> gamesJson)
    given OWrites[FidePlayer] = OWrites: p =>
      Json.obj("ratings" -> p.ratingsMap.mapKeys(_.toString), "year" -> p.year)

private final class RelayPlayerApi(
    tourRepo: RelayTourRepo,
    roundRepo: RelayRoundRepo,
    chapterRepo: lila.study.ChapterRepo,
    chapterPreviewApi: ChapterPreviewApi,
    cacheApi: CacheApi,
    fidePlayerGet: lila.core.fide.GetPlayer
)(using Executor)(using scheduler: Scheduler):
  import RelayPlayer.*

  type RelayPlayers = SeqMap[StudyPlayer.Id, RelayPlayer]

  private val cache = cacheApi[RelayTourId, RelayPlayers](32, "relay.players.data"):
    _.expireAfterWrite(1.minute).buildAsyncFuture(compute)

  private val jsonCache = cacheApi[RelayTourId, JsonStr](32, "relay.players.json"):
    _.expireAfterWrite(1.minute).buildAsyncFuture: tourId =>
      import RelayPlayer.json.given
      cache
        .get(tourId)
        .map: players =>
          JsonStr(Json.stringify(Json.toJson(players.values.toList)))

  export cache.get
  export jsonCache.get as jsonList

  def player(tour: RelayTour, str: String): Fu[Option[JsObject]] =
    val id = FideId.from(str.toIntOption) | PlayerName(str)
    for
      players <- cache.get(tour.id)
      player = players.get(id)
      fidePlayer <- player.flatMap(_.fideId).so(fidePlayerGet)
    yield player.map(json.full(tour)(_, fidePlayer))

  def invalidate(id: RelayTourId) = invalidateDebouncer.push(id)

  private val invalidateDebouncer = Debouncer[RelayTourId](scheduler.scheduleOnce(3.seconds, _), 32): id =>
    import lila.memo.CacheApi.invalidate
    cache.invalidate(id)
    jsonCache.invalidate(id)

  private def compute(tourId: RelayTourId): Fu[RelayPlayers] =
    tourRepo
      .byId(tourId)
      .flatMapz: tour =>
        for
          roundIds     <- roundRepo.idsByTourOrdered(tourId)
          studyPlayers <- fetchStudyPlayers(roundIds)
          rounds       <- chapterRepo.tagsByStudyIds(roundIds.map(_.into(StudyId)))
          players = rounds.foldLeft(SeqMap.empty: RelayPlayers):
            case (players, (studyId, chapters)) =>
              val roundId = studyId.into(RelayRoundId)
              chapters.foldLeft(players):
                case (players, (chapterId, tags)) =>
                  StudyPlayer
                    .fromTags(tags)
                    .flatMap:
                      _.traverse: p =>
                        p.id.flatMap: id =>
                          studyPlayers.get(id).map(id -> _)
                    .fold(players): gamePlayers =>
                      gamePlayers.zipColor.foldLeft(players):
                        case (players, (color, (playerId, player))) =>
                          val (_, opponent) = gamePlayers(!color)
                          val game = RelayPlayer.Game(roundId, chapterId, opponent, color, tags.points)
                          players.updated(
                            playerId,
                            players
                              .getOrElse(playerId, RelayPlayer(player, None, None, None, Vector.empty))
                              .withGame(game)
                          )
          withScore = if tour.showScores then computeScores(players) else players
          withRatingDiff <-
            if tour.showRatingDiffs then computeRatingDiffs(tour.info.fideTcOrGuess, withScore)
            else fuccess(withScore)
        yield withRatingDiff

  type StudyPlayers = SeqMap[StudyPlayer.Id, StudyPlayer.WithFed]
  private def fetchStudyPlayers(roundIds: List[RelayRoundId]): Fu[StudyPlayers] =
    roundIds
      .traverse: roundId =>
        chapterPreviewApi.dataList.uniquePlayers(roundId.into(StudyId))
      .map:
        _.foldLeft(SeqMap.empty: StudyPlayers): (players, roundPlayers) =>
          roundPlayers.foldLeft(players):
            case (players, (id, p)) =>
              if players.contains(id) then players
              else players.updated(id, p.studyPlayer)

  private def computeScores(players: RelayPlayers): RelayPlayers =
    players.view
      .mapValues: p =>
        p.copy(
          score = p.games
            .foldMap(_.playerPoints.so(_.value))
            .some,
          performance = Elo.computePerformanceRating(p.eloGames).map(_.into(IntRating))
        )
      .to(SeqMap)

  private def computeRatingDiffs(tc: FideTC, players: RelayPlayers): Fu[RelayPlayers] =
    players.toList
      .traverse: (id, player) =>
        player.fideId
          .so(fidePlayerGet)
          .map: fidePlayerOpt =>
            for
              fidePlayer <- fidePlayerOpt
              r          <- player.rating.map(_.into(Elo)).orElse(fidePlayer.ratingOf(tc))
              p     = Elo.Player(r, fidePlayer.kFactorOf(tc))
              games = player.eloGames
            yield player.copy(ratingDiff = games.nonEmpty.option(Elo.computeRatingDiff(p, games)))
          .map: newPlayer =>
            id -> (newPlayer | player)
      .map(_.to(SeqMap))
