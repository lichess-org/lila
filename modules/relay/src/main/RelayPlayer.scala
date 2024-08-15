package lila.relay

import chess.{ ByColor, Centis, Color, Elo, FideId, Outcome, PlayerName, PlayerTitle }
import lila.db.dsl.{ *, given }
import lila.study.BSONHandlers.given
import chess.format.pgn.Tags
import lila.study.ChapterPreviewApi
import lila.study.ChapterPreview
import lila.study.StudyPlayer
import lila.study.StudyPlayer.json.given
import scala.collection.immutable.SeqMap
import lila.memo.CacheApi
import play.api.libs.json.*
import lila.core.fide.Federation
import lila.common.Json.given
import lila.common.Debouncer

// Player in a tournament with current performance rating and list of games
case class RelayPlayer(
    player: StudyPlayer.WithFed,
    ratingDiff: Option[Int],
    games: Vector[RelayPlayer.Game]
):
  export player.player.*
  def withGame(game: RelayPlayer.Game) = copy(games = games :+ game)
  def score: Double = games.foldLeft(0d): (score, game) =>
    score + game.playerOutcome.so(_.fold(0.5)(_.so(1d)))

object RelayPlayer:
  case class Game(id: StudyChapterId, opponent: StudyPlayer.WithFed, color: Color, outcome: Option[Outcome]):
    def playerOutcome: Option[Option[Boolean]] =
      outcome.map(_.winner.map(_ == color))

  object json:
    given Writes[Outcome]          = Json.writes
    given Writes[RelayPlayer.Game] = Json.writes
    given OWrites[RelayPlayer] = OWrites: p =>
      Json.toJsObject(p.player) ++ Json.obj(
        "score"  -> p.score,
        "played" -> p.games.size
      )

private final class RelayPlayerApi(
    roundRepo: RelayRoundRepo,
    chapterRepo: lila.study.ChapterRepo,
    chapterPreviewApi: ChapterPreviewApi,
    cacheApi: CacheApi,
    fidePlayerGet: lila.core.fide.GetPlayer
)(using Executor, Scheduler):
  import RelayPlayer.*
  import RelayPlayer.json.given

  type RelayPlayers = SeqMap[StudyPlayer.Id, RelayPlayer]

  private val cache = cacheApi[RelayTourId, RelayPlayers](32, "relay.players.data"):
    _.expireAfterWrite(1 minute).buildAsyncFuture(compute)

  private val jsonCache = cacheApi[RelayTourId, JsonStr](32, "relay.players.json"):
    _.expireAfterWrite(1 minute).buildAsyncFuture: tourId =>
      import RelayPlayer.json.given
      cache
        .get(tourId)
        .map: players =>
          JsonStr(Json.stringify(Json.toJson(players.values.toList)))

  export cache.get
  export jsonCache.{ get as json }

  def invalidate(id: RelayTourId) = invalidateDebouncer.push(id)

  private val invalidateDebouncer = Debouncer[RelayTourId](3 seconds, 32): id =>
    import lila.memo.CacheApi.invalidate
    cache.invalidate(id)
    jsonCache.invalidate(id)

  private def compute(tourId: RelayTourId): Fu[RelayPlayers] = for
    roundIds     <- roundRepo.idsByTourOrdered(tourId)
    studyPlayers <- fetchStudyPlayers(roundIds)
    rounds       <- chapterRepo.tagsByStudyIds(roundIds.map(_.into(StudyId)))
    players = rounds.foldLeft(SeqMap.empty: RelayPlayers):
      case (players, (studyId, chapters)) =>
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
                    val outcome       = tags.outcome
                    val game          = RelayPlayer.Game(chapterId, opponent, color, tags.outcome)
                    players.updated(
                      playerId,
                      players
                        .getOrElse(playerId, RelayPlayer(player, None, Vector.empty))
                        .withGame(game)
                    )
  yield players

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

  private def computeRatingDiffs(tour: RelayTour, players: RelayPlayers): Fu[RelayPlayers] =
    val tc = RelayFidePlayerApi.guessTimeControl(tour)
    players.toList
      .traverse: (id, player) =>
        player.fideId
          .so(fidePlayerGet)
          .map:
            _.fold(id -> player): fidePlayer =>
              val kFactor = fidePlayer.kFactorOf(tc)
              id -> player
      .map(_.to(SeqMap))

  private def computeNewEloRating(
      playerRating: Int,
      opponentRating: Int,
      playerKFactor: Int,
      outcome: Option[Boolean]
  ): Int =
    val playerExpectedScore = 1 / (1 + Math.pow(10, (opponentRating - playerRating) / 400))
    val playerScore         = outcome.fold(0.5d)(_.so(1d))
    val playerNewRating = playerRating + Math.round(playerKFactor * (playerScore - playerExpectedScore)).toInt
    playerNewRating
