package lila.api

import org.apache.pekko.stream.scaladsl.*
import chess.ByColor
import chess.format.Fen
import chess.format.pgn.{ PgnStr, Tag }
import chess.opening.Opening
import play.api.libs.json.*
import play.api.i18n.Lang
import reactivemongo.pekkostream.cursorProducer

import lila.analyse.{ AccuracyPercent, Analysis, JsonView as analysisJson }
import lila.common.HTTPRequest
import lila.common.Json.given
import lila.core.LightUser
import lila.db.dsl.{ *, given }
import lila.game.JsonView.given
import lila.game.PgnDump.{ WithFlags, applyDelay }
import lila.game.{ Divider, Query }
import lila.round.GameProxyRepo
import lila.team.GameTeams
import lila.tournament.Tournament
import lila.gameSearch.GameSearchApi
import smithy4s.time.Timestamp

final class GameApiV2(
    pgnDump: PgnDump,
    gameRepo: lila.game.GameRepo,
    gameCache: lila.game.Cached,
    gameJsonView: lila.game.JsonView,
    pairingRepo: lila.tournament.PairingRepo,
    playerRepo: lila.tournament.PlayerRepo,
    tourName: lila.tournament.GetTourName,
    swissApi: lila.swiss.SwissApi,
    analysisRepo: lila.analyse.AnalysisRepo,
    annotator: lila.analyse.Annotator,
    getLightUser: LightUser.Getter,
    gameProxy: GameProxyRepo,
    divider: Divider,
    gameOpening: lila.game.GameOpening,
    bookmarkApi: lila.bookmark.BookmarkApi,
    gameSearch: GameSearchApi,
    crosstableApi: lila.game.CrosstableApi
)(using Executor, org.apache.pekko.actor.ActorSystem):

  import GameApiV2.*

  def exportOne(game: Game, config: OneConfig)(using Lang): Fu[String] =
    game.pgnImport.ifTrue(config.imported) match
      case Some(imported) => fuccess(imported.pgn.value)
      case None =>
        for
          (game, initialFen, analysis) <- enrich(config.flags)(game)
          opening = gameOpening.atPly(game, true)
          formatted <- config.format match
            case Format.JSON =>
              toJson(game, initialFen, analysis, opening, config).map(Json.stringify)
            case Format.PGN =>
              PgnStr.raw(
                pgnDump(
                  game,
                  initialFen,
                  analysis,
                  opening,
                  config.flags
                ).map(annotator.toPgnString)
              )
        yield formatted

  private val fileR = """[\s,]""".r

  def filename(game: Game, format: Format): Fu[String] =
    gameLightUsers(game).map: users =>
      fileR.replaceAllIn(
        "lichess_pgn_%s_%s_vs_%s.%s.%s".format(
          Tag.UTCDate.format.print(game.createdAt),
          pgnDump.dumper.player.tupled(users.white),
          pgnDump.dumper.player.tupled(users.black),
          game.id,
          format.toString.toLowerCase
        ),
        "_"
      )

  def filename(tour: Tournament, format: Format): String =
    filename(tour, format.toString.toLowerCase)

  def filename(tour: Tournament, format: String): String =
    fileR.replaceAllIn(
      "lichess_tournament_%s_%s_%s.%s".format(
        Tag.UTCDate.format.print(tour.startsAt),
        tour.id,
        scalalib.StringOps.slug(tour.name),
        format
      ),
      "_"
    )

  def filename(swiss: lila.swiss.Swiss, format: Format): String =
    filename(swiss, format.toString.toLowerCase)

  def filename(swiss: lila.swiss.Swiss, format: String): String =
    fileR.replaceAllIn(
      "lichess_swiss_%s_%s_%s.%s".format(
        Tag.UTCDate.format.print(swiss.startsAt),
        swiss.id,
        scalalib.StringOps.slug(swiss.name),
        format
      ),
      "_"
    )

  def exportByUser(config: ByUserConfig)(using Lang): Source[String, ?] =
    val playerSelect =
      if config.finished then config.vs.fold(Query.user(config.user.id)) { Query.opponents(config.user, _) }
      else
        config.vs
          .map(_.id)
          .fold(Query.nowPlaying(config.user.id)):
            Query.nowPlayingVs(config.user.id, _)
    val requiresElasticSearch =
      config.perfKey.nonEmpty || config.analysed.nonEmpty || config.color.nonEmpty || config.rated.nonEmpty
    val gameSource: Source[Game, ?] =
      if requiresElasticSearch then
        import lila.search.Size
        gameSearch
          .idStream(
            config.toGameQuery,
            Size(config.max.fold(500_000)(_.value)),
            config.perSecond.into(MaxPerPage)
          )
          .mapAsync(1)(gameRepo.gamesFromSecondary)
          .mapConcat(identity)
      else
        gameRepo
          .sortedCursor(
            playerSelect ++
              Query.createdBetween(config.since, config.until) ++
              (!config.ongoing).so(Query.finished),
            config.sort.bson,
            batchSize = config.perSecond.value
          )
          .documentSource()
          .take(config.max.fold(Int.MaxValue)(_.value))

    gameSource
      .via(upgradeOngoingGame)
      .via(preparationFlow(config))

  def mobileRecent(user: User)(using Option[Me], Lang): Fu[JsArray] = for
    games <- gameRepo.recentFinishedGamesFromSecondary(user, Max(10))
    config = MobileRecentConfig(user)
    enriched <- games.sequentially(enrich(config.flags))
    jsons <- enriched.sequentially: (game, fen, analysis) =>
      toJson(game, fen, analysis, gameOpening.atPly(game, false), config)
  yield JsArray(jsons)

  def mobileCurrent(user: User)(using Option[Me], Lang): Fu[Option[JsObject]] =
    gameCache
      .lastPlayedPlayingId(user.id)
      .flatMapz(gameProxy.gameIfPresentOrFetch)
      .flatMapz: game =>
        val config = OneConfig(GameApiV2.Format.JSON, false, WithFlags())
        enrich(config.flags)(game).flatMap: (game, fen, analysis) =>
          toJson(game, fen, analysis, none, config).dmap(some)

  def exportByIds(config: ByIdsConfig)(using Lang): Source[String, ?] =
    gameRepo
      .sortedCursor(
        $inIds(config.ids),
        Query.sortCreated,
        hint = $id(1).some,
        batchSize = config.perSecond.value
      )
      .documentSource()
      .via(upgradeOngoingGame)
      .via(preparationFlow(config))

  def exportByTournament(config: ByTournamentConfig, onlyUserId: Option[UserId])(using
      Lang
  ): Source[String, ?] =
    pairingRepo
      .sortedCursor(
        tournamentId = config.tour.id,
        userId = onlyUserId,
        batchSize = config.perSecond.value
      )
      .documentSource()
      .grouped(30)
      .mapAsync(1): pairings =>
        config.tour.isTeamBattle
          .so:
            playerRepo.teamsOfPlayers(config.tour.id, pairings.flatMap(_.users).distinct).dmap(_.toMap)
          .flatMap: playerTeams =>
            gameRepo
              .gameOptionsFromSecondary(pairings.map(_.gameId))
              .map:
                _.zip(pairings).collect { case (Some(game), pairing) =>
                  (
                    game,
                    pairing,
                    ByColor(pairing.user1, pairing.user2).traverse(playerTeams.get)
                  )
                }
      .mapConcat(identity)
      .throttle(config.perSecond.value, 1.second)
      .mapAsync(4): (game, pairing, teams) =>
        enrich(config.flags)(game).dmap { (_, pairing, teams) }
      .mapAsync(4) { case ((game, fen, analysis), pairing, teams) =>
        val opening = config.flags.opening.isDefined.so(gameOpening.atPly(game, false))
        config.format match
          case Format.PGN => pgnDump.formatter(config.flags)(game, fen, analysis, opening, teams)
          case Format.JSON =>
            def addBerserk(color: Color)(json: JsObject) =
              if pairing.berserkOf(color) then
                json.deepMerge:
                  Json.obj:
                    "players" -> Json.obj(color.name -> Json.obj("berserk" -> true))
              else json
            toJson(game, fen, analysis, opening, config, teams)
              .dmap(addBerserk(chess.White))
              .dmap(addBerserk(chess.Black))
              .dmap: json =>
                s"${Json.stringify(json)}\n"
      }

  def exportBySwiss(config: BySwissConfig)(using Lang): Source[String, ?] =
    swissApi
      .gameIdSource(
        swissId = config.swissId,
        player = config.player,
        batchSize = config.perSecond.value
      )
      .grouped(30)
      .mapAsync(1)(gameRepo.gamesFromSecondary)
      .mapConcat(identity)
      .via(preparationFlow(config))

  def exportUserImportedGames(user: User): Source[PgnStr, ?] =
    gameRepo
      .sortedCursor(Query.imported(user.id), Query.importedSort, batchSize = 20)
      .documentSource()
      .throttle(20, 1.second)
      .mapConcat(_.pgnImport.map(_.pgn.map(_ + "\n\n\n")).toList)

  def exportUserBookmarks(config: BookmarkConfig)(using Lang): Source[String, ?] =
    import lila.game.BSONHandlers.gameHandler
    bookmarkApi.coll
      .aggregateWith[Game](readPreference = ReadPref.sec): framework =>
        import framework.*
        List(
          Match(bookmarkApi.userIdQuery(config.user) ++ dateBetween("d", config.since, config.until)),
          Sort(if config.sort == GameSort.DateDesc then Descending("d") else Ascending("d")),
          Limit(config.max.fold(5000)(_.value)),
          PipelineOperator($lookup.simple(gameRepo.coll, "game", "g", "_id")),
          Unwind("game"),
          ReplaceRootField("game")
        )
      .documentSource()
      .via(preparationFlow(config))

  def crosstableWith(user: User)(me: Me): Fu[JsObject] =
    crosstableApi.withMatchup(me.userId, user.id).map(Json.toJsObject)

  private val upgradeOngoingGame =
    Flow[Game].mapAsync(4)(gameProxy.upgradeIfPresent)

  private def preparationFlow(config: Config)(using Lang) =
    Flow[Game]
      .throttle(config.perSecond.value, 1.second)
      .mapAsync(4)(enrich(config.flags))
      .mapAsync(4): (game, fen, analysis) =>
        val opening = config.flags.opening.so(gameOpening.atPly(game, _))
        formatterFor(config)(game, fen, analysis, opening, None)

  private def enrich(flags: WithFlags)(game: Game) =
    gameRepo
      .initialFen(game)
      .flatMap: initialFen =>
        flags.requiresAnalysis
          .so(analysisRepo.byGame(game))
          .dmap:
            (game, initialFen, _)

  private def formatterFor(config: Config)(using Lang) =
    config.format match
      case Format.PGN => pgnDump.formatter(config.flags)
      case Format.JSON => jsonFormatter(config)

  private def jsonFormatter(config: Config)(using Lang) =
    (
        game: Game,
        initialFen: Option[Fen.Full],
        analysis: Option[Analysis],
        opening: Option[Opening.AtPly],
        teams: Option[GameTeams]
    ) =>
      toJson(game, initialFen, analysis, opening, config, teams).map: json =>
        s"${Json.stringify(json)}\n"

  private def toJson(
      g: Game,
      initialFen: Option[Fen.Full],
      analysisOption: Option[Analysis],
      opening: Option[Opening.AtPly],
      config: Config,
      teams: Option[GameTeams] = None
  )(using Lang): Fu[JsObject] = for
    lightUsers <- gameLightUsers(g)
    flags = config.flags
    pgn <- config.flags.pgnInJson.optionFu:
      pgnDump(g, initialFen, analysisOption, opening, config.flags).map(annotator.toPgnString)
    bookmarked <- config.flags.bookmark.so(bookmarkApi.exists(g, config.by.map(_.userId)))
    arena <- g.tournamentId.traverse: tournamentId =>
      for name <- tourName.async(tournamentId)
      yield Json.obj("id" -> tournamentId, "name" -> name)
    division = flags.division.option(divider(g, initialFen))
    accuracy = analysisOption
      .ifTrue(flags.accuracy)
      .flatMap(AccuracyPercent.gameAccuracy(g.startedAtPly.turn, _))
    phases = flags.accuracy.so:
      (division, analysisOption).mapN(AccuracyPercent.phaseAccuracies(_, _))
  yield Json
    .obj(
      "id" -> g.id,
      "rated" -> g.rated,
      "variant" -> g.variant.key,
      "speed" -> g.speed.key,
      "perf" -> g.perfKey,
      "createdAt" -> g.createdAt,
      "lastMoveAt" -> g.movedAt,
      "status" -> g.status.name,
      "source" -> g.source,
      "players" -> JsObject(lightUsers.mapList: (p, user) =>
        p.color.name -> gameJsonView
          .player(p, user)
          .add:
            "analysis" -> analysisOption.flatMap:
              analysisJson.player(g.pov(p.color).sideAndStart)(_, accuracy, ~phases)
          .add("team" -> teams.map(_(p.color))))
    )
    .add("fullId" -> config.by.flatMap(Pov(g, _)).map(_.fullId))
    .add("initialFen" -> initialFen)
    .add("winner" -> g.winnerColor.map(_.name))
    .add("opening" -> opening)
    .add("moves" -> flags.moves.option {
      applyDelay(g.sans, flags.keepDelayIf(g.playable)).mkString(" ")
    })
    .add("clocks" -> flags.clocks.so(g.bothClockStates).map { clocks =>
      applyDelay(clocks, flags.keepDelayIf(g.playable))
    })
    .add("pgn" -> pgn)
    .add("daysPerTurn" -> g.daysPerTurn)
    .add("analysis" -> analysisOption.ifTrue(flags.evals).map(analysisJson.moves(_, withGlyph = false)))
    .add("arenaTour" -> arena)
    .add("swissTour" -> g.swissId.map(id => Json.obj("id" -> id)))
    .add("clock" -> g.clock.map: clock =>
      Json.obj(
        "initial" -> clock.limitSeconds,
        "increment" -> clock.incrementSeconds,
        "totalTime" -> clock.estimateTotalSeconds
      ))
    .add("lastFen" -> flags.lastFen.option(Fen.write(g.chess.position)))
    .add("lastMove" -> flags.lastFen.option(g.lastMoveKeys))
    .add("division" -> division)
    .add("bookmarked" -> bookmarked)
    .add("import" -> g.pgnImport.map: i =>
      Json.obj().add("date" -> i.date))

  private def gameLightUsers(game: Game): Future[ByColor[(lila.core.game.Player, Option[LightUser])]] =
    game.players.traverse(_.userId.so(getLightUser)).dmap(game.players.zip(_))

object GameApiV2:

  enum Format:
    case PGN, JSON
  object Format:
    def byRequest(using req: play.api.mvc.RequestHeader) =
      if HTTPRequest.acceptsNdJson(req) || HTTPRequest.acceptsJson(req)
      then JSON
      else PGN

  sealed trait Config:
    val format: Format
    val flags: WithFlags
    val by: Option[Me]
    val perSecond: MaxPerSecond

  enum GameSort(val bson: Bdoc):
    case DateAsc extends GameSort(Query.sortChronological)
    case DateDesc extends GameSort(Query.sortAntiChronological)

  case class OneConfig(
      format: Format,
      imported: Boolean,
      flags: WithFlags
  )(using val by: Option[Me])
      extends Config:
    val perSecond = MaxPerSecond(1)

  case class ByUserConfig(
      user: User,
      vs: Option[User],
      format: Format,
      since: Option[Instant] = None,
      until: Option[Instant] = None,
      max: Option[Max] = None,
      rated: Option[Boolean] = None,
      perfKey: Set[PerfKey],
      analysed: Option[Boolean] = None,
      color: Option[Color],
      flags: WithFlags,
      sort: GameSort,
      perSecond: MaxPerSecond,
      ongoing: Boolean = false,
      finished: Boolean = true
  )(using val by: Option[Me])
      extends Config:
    import lila.search.spec.DateRange
    import lila.gameSearch.*

    private def ts(i: Instant): Timestamp = Timestamp.fromEpochMilli(i.toEpochMilli)

    def toSorting =
      sort match
        case GameSort.DateAsc => SearchSort(Fields.date, "asc")
        case GameSort.DateDesc => SearchSort(Fields.date, "desc")

    def toGameQuery =
      SearchData(
        players = SearchPlayer(
          a = user.id.into(UserStr).some,
          b = vs.map(_.id.into(UserStr)),
          white = color.exists(_.white).option(user.id.into(UserStr)),
          black = color.exists(_.black).option(user.id.into(UserStr))
        ),
        analysed = analysed,
        sort = toSorting.some
      ).query.copy(
        date = DateRange(since.map(ts), until.map(ts)),
        perf = perfKey.view.map(_.id.value).toList,
        rated = rated
      )

  case class ByIdsConfig(
      ids: Seq[GameId],
      format: Format,
      flags: WithFlags,
      perSecond: MaxPerSecond,
      playerFile: Option[String] = None
  )(using val by: Option[Me])
      extends Config

  case class ByTournamentConfig(
      tour: Tournament,
      format: Format,
      flags: WithFlags,
      perSecond: MaxPerSecond
  )(using val by: Option[Me])
      extends Config

  case class BySwissConfig(
      swissId: SwissId,
      format: Format,
      flags: WithFlags,
      perSecond: MaxPerSecond,
      player: Option[UserId]
  )(using val by: Option[Me])
      extends Config

  case class BookmarkConfig(
      user: UserId,
      format: Format,
      since: Option[Instant] = None,
      until: Option[Instant] = None,
      max: Option[Max] = None,
      flags: WithFlags,
      sort: GameSort,
      perSecond: MaxPerSecond
  )(using val by: Option[Me])
      extends Config

  case class MobileRecentConfig(user: User)(using val by: Option[Me]) extends Config:
    val format = GameApiV2.Format.JSON
    val flags =
      WithFlags(
        clocks = false,
        moves = false,
        evals = false,
        lastFen = true,
        accuracy = true
      )
    val perSecond = MaxPerSecond(20) // unused
