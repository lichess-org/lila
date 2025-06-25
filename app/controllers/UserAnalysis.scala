package controllers

import chess.format.Fen
import chess.variant.{ FromPosition, Standard, Variant, Chess960 }
import chess.{ Position, ByColor }
import play.api.libs.json.Json
import play.api.mvc.*

import lila.app.{ *, given }
import lila.common.HTTPRequest
import lila.core.id.GameFullId
import lila.tree.ExportOptions

final class UserAnalysis(
    env: Env,
    gameC: => Game
) extends LilaController(env)
    with lila.web.TheftPrevention:

  def index = load(none, Standard)

  def parseArg(arg: String) =
    arg.split("/", 2) match
      case Array(key)      => load(none, Variant.orDefault(Variant.LilaKey(key)))
      case Array(key, fen) =>
        Variant(Variant.LilaKey(key)) match
          case Some(variant) if variant != Standard            => load(fen.some, variant)
          case _ if Fen.Full.clean(fen) == Standard.initialFen => load(none, Standard)
          case Some(Standard)                                  => load(fen.some, FromPosition)
          case _                                               => load(arg.some, FromPosition)
      case _ => load(none, Standard)

  private def load(urlFen: Option[String], variant: Variant) = Open:
    val inputFen: Option[Fen.Full]       = urlFen.orElse(get("fen")).flatMap(readFen)
    val chess960PositionNum: Option[Int] = variant.chess960.so:
      getInt("position").orElse: // no input fen or num defaults to standard start position
        Chess960.positionNumber(inputFen | variant.initialFen)
    val decodedFen: Option[Fen.Full] = chess960PositionNum.flatMap(Chess960.positionToFen).orElse(inputFen)
    val pov                          = makePov(decodedFen, variant)
    val orientation                  = get("color").flatMap(Color.fromName) | pov.color
    for
      data <- env.api.roundApi.userAnalysisJson(
        pov,
        ctx.pref,
        decodedFen,
        orientation,
        owner = false
      )
      page <- renderPage(views.analyse.ui.userAnalysis(data, pov, chess960PositionNum))
    yield Ok(page)
      .withCanonical(routes.UserAnalysis.index)
      .enforceCrossSiteIsolation

  def pgn(pgn: String) = Open:
    val pov         = makePov(none, Standard)
    val orientation = get("color").flatMap(Color.fromName) | pov.color
    val decodedPgn  =
      lila.common.String
        .decodeUriPath(pgn.take(5000))
        .map(_.replace("_", " ").replace("+", " ").trim)
        .filter(_.nonEmpty)
    Ok.async:
      env.api.roundApi
        .userAnalysisJson(pov, ctx.pref, none, orientation, owner = false)
        .map: data =>
          views.analyse.ui.userAnalysis(data, pov, inlinePgn = decodedPgn)
    .map(_.enforceCrossSiteIsolation)

  def embed = Anon:
    InEmbedContext:
      val pov         = makePov(none, Standard)
      val orientation = get("color").flatMap(Color.fromName) | pov.color
      val fen         = get("fen").flatMap(readFen)
      env.api.roundApi
        .userAnalysisJson(pov, ctx.pref, fen, orientation, owner = false)
        .map: data =>
          Ok(views.analyse.embed.userAnalysis(data)).enforceCrossSiteIsolation

  def readFen(from: String): Option[Fen.Full] = lila.common.String
    .decodeUriPath(from)
    .filter(_.trim.nonEmpty)
    .map(Fen.Full.clean)

  private[controllers] def makePov(fen: Option[Fen.Full], variant: Variant): Pov =
    makePov:
      Position.AndFullMoveNumber(variant, fen.filter(_.value.nonEmpty))

  private[controllers] def makePov(from: Position.AndFullMoveNumber): Pov =
    Pov(
      lila.core.game
        .newGame(
          chess = chess.Game(position = from.position, ply = from.ply),
          players = ByColor(lila.game.Player.make(_, none)),
          rated = chess.Rated.No,
          source = lila.core.game.Source.Api,
          pgnImport = None
        )
        .withId(lila.game.Game.syntheticId),
      from.position.color
    )

  // correspondence premove aka forecast
  def game(id: GameId, color: Color) = Open:
    Found(env.game.gameRepo.game(id)): g =>
      env.round.proxyRepo.upgradeIfPresent(g).flatMap { game =>
        val pov = Pov(game, color)
        negotiateApi(
          html =
            if game.replayable then Redirect(routes.Round.watcher(game.id, color))
            else
              val owner = isMyPov(pov)
              for
                initialFen <- env.game.gameRepo.initialFen(game.id)
                data       <-
                  env.api.roundApi
                    .userAnalysisJson(pov, ctx.pref, initialFen, pov.color, owner = owner)
                withForecast = owner && !pov.game.synthetic && pov.game.playable
                page <- renderPage:
                  views.analyse.ui.userAnalysis(data, pov, withForecast = withForecast)
              yield Ok(page).noCache
          ,
          api = _ => mobileAnalysis(pov)
        )
      }

  private def mobileAnalysis(pov: Pov)(using ctx: Context): Fu[Result] = for
    initialFen <- env.game.gameRepo.initialFen(pov.gameId)
    users      <- env.user.api.gamePlayers.analysis(pov.game)
    owner = isMyPov(pov)
    _     = gameC.preloadUsers(users)
    analysis   <- env.analyse.analyser.get(pov.game)
    crosstable <- env.game.crosstableApi(pov.game)
    data       <- env.api.roundApi.review(
      pov,
      users,
      tv = none,
      analysis,
      initialFen = initialFen,
      withFlags = ExportOptions(
        division = true,
        opening = true,
        clocks = true,
        movetimes = true,
        rating = ctx.pref.showRatings,
        lichobileCompat = HTTPRequest.isLichobile(ctx.req)
      ),
      owner = owner
    )
  yield
    import lila.game.JsonView.given
    Ok(data.add("crosstable", crosstable))

  private def forecastReload = JsonOk(Json.obj("reload" -> true))

  def forecastsPost(fullId: GameFullId) = AuthOrScopedBodyWithParser(parse.json)(_.Web.Mobile) { ctx ?=> _ ?=>
    import lila.round.Forecast
    Found(env.round.proxyRepo.pov(fullId)): pov =>
      if isTheft(pov) then theftResponse
      else
        ctx.body.body
          .validate[Forecast.Steps]
          .fold(
            err => BadRequest(err.toString),
            forecasts =>
              val fu = for
                _   <- env.round.forecastApi.save(pov, forecasts)
                res <- env.round.forecastApi.loadForDisplay(pov)
              yield res.fold(JsonOk(Json.obj("none" -> true)))(JsonOk(_))
              fu.recover:
                case Forecast.OutOfSync             => forecastReload
                case _: lila.core.round.ClientError => forecastReload
          )
  }

  def forecastsGet(fullId: GameFullId) = Scoped(_.Web.Mobile) { _ ?=> _ ?=>
    Found(env.round.proxyRepo.pov(fullId)): pov =>
      JsonOk(env.round.mobile.forecast(pov.game, pov.fullId.anyId))
  }

  def forecastsOnMyTurn(fullId: GameFullId, uci: String) =
    AuthOrScopedBodyWithParser(parse.json)(_.Web.Mobile) { ctx ?=> _ ?=>
      import lila.round.Forecast
      Found(env.round.proxyRepo.pov(fullId)): pov =>
        if isTheft(pov) then theftResponse
        else
          ctx.body.body
            .validate[Forecast.Steps]
            .fold(
              err => BadRequest(err.toString),
              forecasts =>
                for
                  _ <- env.round.forecastApi.playAndSave(pov, uci, forecasts).recoverDefault
                  wait = (1 + Forecast.maxPlies(forecasts).min(10)) * 50
                  _ <- lila.common.LilaFuture.sleep(wait.millis)
                yield forecastReload
            )
    }

  def help = Open:
    Ok.snip:
      lila.web.ui.help.analyse(getBool("study"))
