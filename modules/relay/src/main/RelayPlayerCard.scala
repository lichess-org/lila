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

// Player in a tournament with current performance rating and list of games
case class RelayPlayerCard(
    player: StudyPlayer,
    ratingDiff: Option[Int],
    games: Vector[RelayPlayerCard.Game]
):
  def withGame(game: RelayPlayerCard.Game) = copy(games = games :+ game)

object RelayPlayerCard:
  case class Game(id: StudyChapterId, opponent: StudyPlayer, color: Color, outcome: Option[Outcome]):
    def playerOutcome =
      if color.white then outcome
      else outcome.map(o => o.copy(winner = o.winner.map(!_)))

  object json:
    given Writes[Outcome]                                            = Json.writes
    given (using Federation.ByFideIds): Writes[RelayPlayerCard.Game] = Json.writes
    given (using Federation.ByFideIds): Writes[RelayPlayerCard]      = Json.writes

private final class RelayPlayerCardApi(
    colls: RelayColls,
    roundRepo: RelayRoundRepo,
    chapterRepo: lila.study.ChapterRepo,
    chapterPreviewApi: ChapterPreviewApi,
    federationsOf: Federation.FedsOf,
    cacheApi: CacheApi
)(using Executor, Scheduler):
  import RelayPlayerCard.*
  import RelayPlayerCard.json.given

  def playersOf(tour: RelayTourId): Fu[SeqMap[StudyPlayer.Id, StudyPlayer]] =
    roundRepo
      .idsByTourOrdered(tour)
      .flatMap:
        _.traverse: roundId =>
          chapterPreviewApi
            .dataList(roundId.into(StudyId))
            .map: games =>
              games.flatMap(_.players.so(_.toList))
      .map(_.flatten.distinct)
      .map:
        _.foldLeft(SeqMap.empty[StudyPlayer.Id, StudyPlayer]): (players, p) =>
          p.player.id.fold(players): id =>
            if players.contains(id) then players
            else players.updated(id, p.player)

  def cardsJson(tour: RelayTour): Fu[JsonStr] = cache.get(tour.id)

  private val invalidateDebouncer =
    lila.common.Debouncer[RelayTourId](3 seconds, 32)(id => cache.put(id, computeJson(id)))

  def invalidate(id: RelayTourId) = invalidateDebouncer.push(id)

  private val cache = cacheApi[RelayTourId, JsonStr](32, "relay.leaderboard"):
    _.expireAfterWrite(10 minutes).buildAsyncFuture(computeJson)

  private def computeJson(id: RelayTourId): Fu[JsonStr] = for
    cards <- cardsOf(id)
    feds  <- federationsOf(cards.flatMap(_.player.fideId))
    given Federation.ByFideIds = feds
  yield JsonStr(Json.stringify(Json.toJson(cards)))

  def cardsOf(tourId: RelayTourId): Fu[List[RelayPlayerCard]] =
    playersOf(tourId).flatMap: players =>
      colls.round
        .aggregateList(300): framework =>
          import framework.*
          Match($doc("tourId" -> tourId)) -> List(
            Sort(Ascending("order")),
            Project($doc("_id" -> 1)),
            PipelineOperator:
              $lookup.pipeline(
                chapterRepo.coll,
                "games",
                "_id",
                "studyId",
                List(
                  $doc("$sort"    -> $sort.asc("order")),
                  $doc("$project" -> $doc("tags" -> 1))
                )
              )
          )
        .map: docs =>
          for
            doc     <- docs
            roundId <- doc.getAsOpt[RelayRoundId]("_id").toList
            games = for
              doc  <- ~doc.getAsOpt[List[Bdoc]]("games")
              id   <- doc.getAsOpt[StudyChapterId]("_id")
              tags <- doc.getAsOpt[Tags]("tags")
            yield (id, tags)
          yield (roundId, games)
        .map: rounds =>
          rounds
            .foldLeft(SeqMap[StudyPlayer.Id, RelayPlayerCard]()):
              case (players, (roundId, games)) =>
                games.foldLeft(players):
                  case (players, (gameId, tags)) =>
                    StudyPlayer
                      .fromTags(tags)
                      .fold(players): gamePlayers =>
                        gamePlayers.zipColor.foldLeft(players):
                          case (players, (color, gamePlayer)) =>
                            gamePlayer.id.fold(players): playerId =>
                              val opponent = gamePlayers(!color)
                              val outcome  = tags.outcome
                              val game     = Game(gameId, opponent, color, tags.outcome)
                              players.updated(
                                playerId,
                                players
                                  .getOrElse(playerId, RelayPlayerCard(gamePlayer, None, Vector.empty))
                                  .withGame(game)
                              )
        .map:
          _.values.toList
