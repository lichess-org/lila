package controllers

import chess.format.Fen
import chess.variant.{ FromPosition, Standard, Variant }
import chess.{ Black, Situation, White, FullMoveNumber }
import play.api.libs.json.Json
import play.api.mvc.*
import views.*

import lila.app.{ given, * }
import lila.game.Pov
import lila.round.JsonView.WithFlags

final class UserAnalysis(
    env: Env,
    gameC: => Game
) extends LilaController(env)
    with TheftPrevention:

  def index = load("", Standard)

  def parseArg(arg: String) =
    arg.split("/", 2) match
      case Array(key) => load("", Variant.orDefault(Variant.LilaKey(key)))
      case Array(key, fen) =>
        Variant(Variant.LilaKey(key)) match
          case Some(variant) if variant != Standard           => load(fen, variant)
          case _ if Fen.Epd.clean(fen) == Standard.initialFen => load("", Standard)
          case Some(Standard)                                 => load(fen, FromPosition)
          case _                                              => load(arg, FromPosition)
      case _ => load("", Standard)

  def load(urlFen: String, variant: Variant) = Open:
    val decodedFen: Option[Fen.Epd] = lila.common.String
      .decodeUriPath(urlFen)
      .filter(_.trim.nonEmpty)
      .orElse(get("fen")) map Fen.Epd.clean
    val pov         = makePov(decodedFen, variant)
    val orientation = get("color").flatMap(chess.Color.fromName) | pov.color
    for
      data <- env.api.roundApi.userAnalysisJson(
        pov,
        ctx.pref,
        decodedFen,
        orientation,
        owner = false,
        me = ctx.me
      )
      page <- renderPage(html.board.userAnalysis(data, pov))
    yield Ok(page)
      .withCanonical(routes.UserAnalysis.index)
      .enableSharedArrayBuffer

  def pgn(pgn: String) = Open:
    val pov         = makePov(none, Standard)
    val orientation = get("color").flatMap(chess.Color.fromName) | pov.color
    Ok.pageAsync:
      env.api.roundApi
        .userAnalysisJson(pov, ctx.pref, none, orientation, owner = false, me = ctx.me) map { data =>
        html.board.userAnalysis(data, pov, inlinePgn = pgn.replace("_", " ").some)
      }
    .map(_.enableSharedArrayBuffer)

  private[controllers] def makePov(fen: Option[Fen.Epd], variant: Variant): Pov =
    makePov:
      fen.filter(_.value.nonEmpty).flatMap {
        Fen.readWithMoveNumber(variant, _)
      } | Situation.AndFullMoveNumber(Situation(variant), FullMoveNumber.initial)

  private[controllers] def makePov(from: Situation.AndFullMoveNumber): Pov =
    Pov(
      lila.game.Game
        .make(
          chess = chess.Game(
            situation = from.situation,
            ply = from.ply
          ),
          whitePlayer = lila.game.Player.make(White, none),
          blackPlayer = lila.game.Player.make(Black, none),
          mode = chess.Mode.Casual,
          source = lila.game.Source.Api,
          pgnImport = None
        )
        .withId(lila.game.Game.syntheticId),
      from.situation.color
    )

  // correspondence premove aka forecast
  def game(id: GameId, color: String) = Open:
    Found(env.game.gameRepo game id): g =>
      env.round.proxyRepo upgradeIfPresent g flatMap { game =>
        val pov = Pov(game, chess.Color.fromName(color) | White)
        negotiateApi(
          html =
            if game.replayable then Redirect(routes.Round.watcher(game.id, color))
            else
              val owner = isMyPov(pov)
              for
                initialFen <- env.game.gameRepo initialFen game.id
                data <-
                  env.api.roundApi
                    .userAnalysisJson(pov, ctx.pref, initialFen, pov.color, owner = owner, me = ctx.me)
                withForecast = owner && !pov.game.synthetic && pov.game.playable
                page <- renderPage:
                  html.board.userAnalysis(data, pov, withForecast = withForecast)
              yield Ok(page).noCache
          ,
          api = apiVersion => mobileAnalysis(pov, apiVersion)
        )
      }

  private def mobileAnalysis(pov: Pov, apiVersion: lila.common.ApiVersion)(using
      ctx: Context
  ): Fu[Result] =
    env.game.gameRepo initialFen pov.gameId flatMap { initialFen =>
      val owner = isMyPov(pov)
      gameC.preloadUsers(pov.game) zip
        (env.analyse.analyser get pov.game) zip
        env.game.crosstableApi(pov.game) flatMap { case ((_, analysis), crosstable) =>
          import lila.game.JsonView.given
          env.api.roundApi.review(
            pov,
            apiVersion,
            tv = none,
            analysis,
            initialFen = initialFen,
            withFlags = WithFlags(
              division = true,
              opening = true,
              clocks = true,
              movetimes = true,
              rating = ctx.pref.showRatings
            ),
            owner = owner
          ) map { data =>
            Ok(data.add("crosstable", crosstable))
          }
        }
    }

  private def forecastReload = JsonOk(Json.obj("reload" -> true))

  def forecasts(fullId: GameFullId) = AuthBody(parse.json) { ctx ?=> _ ?=>
    import lila.round.Forecast
    Found(env.round.proxyRepo pov fullId): pov =>
      if isTheft(pov) then theftResponse
      else
        ctx.body.body
          .validate[Forecast.Steps]
          .fold(
            err => BadRequest(err.toString),
            forecasts =>
              env.round.forecastApi.save(pov, forecasts) >>
                env.round.forecastApi.loadForDisplay(pov) map {
                  _.fold(JsonOk(Json.obj("none" -> true)))(JsonOk(_))
                } recover {
                  case Forecast.OutOfSync        => forecastReload
                  case _: lila.round.ClientError => forecastReload
                }
          )
  }

  def forecastsOnMyTurn(fullId: GameFullId, uci: String) =
    AuthBody(parse.json) { ctx ?=> _ ?=>
      import lila.round.Forecast
      Found(env.round.proxyRepo pov fullId): pov =>
        if isTheft(pov) then theftResponse
        else
          ctx.body.body
            .validate[Forecast.Steps]
            .fold(
              err => BadRequest(err.toString),
              forecasts =>
                val wait = 50 + (Forecast maxPlies forecasts min 10) * 50
                env.round.forecastApi.playAndSave(pov, uci, forecasts).recoverDefault >>
                  lila.common.LilaFuture.sleep(wait.millis) inject
                  forecastReload
            )
    }

  def help = Open:
    Ok.page:
      html.site.helpModal.analyse(getBool("study"))
