package controllers

import chess.format.Forsyth.SituationPlus
import chess.format.{ FEN, Forsyth }
import chess.Situation
import chess.variant.{ Variant, Standard, FromPosition }
import play.api.libs.json.Json
import play.api.mvc._
import scala.concurrent.duration._

import lila.api.Context
import lila.app._
import lila.game.{ GameRepo, Pov }
import lila.i18n.I18nKeys
import lila.round.Forecast.{ forecastStepJsonFormat, forecastJsonWriter }
import lila.round.JsonView.WithFlags
import views._

object UserAnalysis extends LilaController with TheftPrevention {

  def index = load("", Standard)

  def parse(arg: String) = arg.split("/", 2) match {
    case Array(key) => load("", Variant orDefault key)
    case Array(key, fen) => Variant.byKey get key match {
      case Some(variant) => load(fen, variant)
      case _ if fen == Standard.initialFen => load(arg, Standard)
      case _ => load(arg, FromPosition)
    }
    case _ => load("", Standard)
  }

  def load(urlFen: String, variant: Variant) = Open { implicit ctx =>
    val decodedFen: Option[FEN] = lila.common.String.decodeUriPath(urlFen)
      .map(_.replace('_', ' ').trim).filter(_.nonEmpty)
      .orElse(get("fen")) map FEN.apply
    val pov = makePov(decodedFen, variant)
    val orientation = get("color").flatMap(chess.Color.apply) | pov.color
    Env.api.roundApi.userAnalysisJson(pov, ctx.pref, decodedFen, orientation, owner = false, me = ctx.me) map { data =>
      Ok(html.board.userAnalysis(data, pov))
    }
  }

  private[controllers] def makePov(fen: Option[FEN], variant: Variant): Pov = makePov {
    fen.filter(_.value.nonEmpty).flatMap { f =>
      Forsyth.<<<@(variant, f.value)
    } | SituationPlus(Situation(variant), 1)
  }

  private[controllers] def makePov(from: SituationPlus): Pov = Pov(
    lila.game.Game.make(
      chess = chess.Game(
        situation = from.situation,
        turns = from.turns
      ),
      whitePlayer = lila.game.Player.make(chess.White, none),
      blackPlayer = lila.game.Player.make(chess.Black, none),
      mode = chess.Mode.Casual,
      source = lila.game.Source.Api,
      pgnImport = None
    ).withId("synthetic"),
    from.situation.color
  )

  def game(id: String, color: String) = Open { implicit ctx =>
    OptionFuResult(GameRepo game id) { game =>
      val pov = Pov(game, chess.Color(color == "white"))
      negotiate(
        html =
          if (game.replayable) Redirect(routes.Round.watcher(game.id, color)).fuccess
          else for {
            initialFen <- GameRepo initialFen game.id
            data <- Env.api.roundApi.userAnalysisJson(pov, ctx.pref, initialFen, pov.color, owner = isMyPov(pov), me = ctx.me)
          } yield NoCache(Ok(html.board.userAnalysis(data, pov))),
        api = apiVersion => mobileAnalysis(pov, apiVersion)
      )
    }
  }

  private def mobileAnalysis(pov: Pov, apiVersion: lila.common.ApiVersion)(implicit ctx: Context): Fu[Result] =
    GameRepo initialFen pov.gameId flatMap { initialFen =>
      Game.preloadUsers(pov.game) zip
        (Env.analyse.analyser get pov.game) zip
        Env.game.crosstableApi(pov.game) zip
        Env.bookmark.api.exists(pov.game, ctx.me) flatMap {
          case _ ~ analysis ~ crosstable ~ bookmarked =>
            import lila.game.JsonView.crosstableWrites
            Env.api.roundApi.review(pov, apiVersion,
              tv = none,
              analysis,
              initialFenO = initialFen.some,
              withFlags = WithFlags(division = true, opening = true, clocks = true, movetimes = true)) map { data =>
                Ok(data.add("crosstable", crosstable))
              }
        }
    }

  def socket(apiVersion: Int) = SocketOption { implicit ctx =>
    getSocketUid("sri") ?? { uid =>
      Env.analyse.socketHandler.join(uid, ctx.me, apiVersion) map some
    }
  }

  // XHR only
  def pgn = OpenBody { implicit ctx =>
    implicit val req = ctx.body
    Env.importer.forms.importForm.bindFromRequest.fold(
      jsonFormError,
      data => Env.importer.importer.inMemory(data).fold(
        err => BadRequest(jsonError(err.shows)).fuccess, {
          case (game, fen) =>
            val pov = Pov(game, chess.White)
            Env.api.roundApi.userAnalysisJson(pov, ctx.pref, initialFen = fen, pov.color, owner = false, me = ctx.me) map { data =>
              Ok(data)
            }
        }
      )
    ).map(_ as JSON)
  }

  def forecasts(fullId: String) = AuthBody(BodyParsers.parse.json) { implicit ctx => me =>
    import lila.round.Forecast
    OptionFuResult(GameRepo pov fullId) { pov =>
      if (isTheft(pov)) fuccess(theftResponse)
      else ctx.body.body.validate[Forecast.Steps].fold(
        err => BadRequest(err.toString).fuccess,
        forecasts => Env.round.forecastApi.save(pov, forecasts) >>
          Env.round.forecastApi.loadForDisplay(pov) map {
            case None => Ok(Json.obj("none" -> true))
            case Some(fc) => Ok(Json toJson fc) as JSON
          } recover {
            case Forecast.OutOfSync => Ok(Json.obj("reload" -> true))
          }
      )
    }
  }

  def forecastsOnMyTurn(fullId: String, uci: String) = AuthBody(BodyParsers.parse.json) { implicit ctx => me =>
    import lila.round.Forecast
    OptionFuResult(GameRepo pov fullId) { pov =>
      if (isTheft(pov)) fuccess(theftResponse)
      else {
        ctx.body.body.validate[Forecast.Steps].fold(
          err => BadRequest(err.toString).fuccess,
          forecasts => {
            def wait = 50 + (Forecast maxPlies forecasts min 10) * 50
            Env.round.forecastApi.playAndSave(pov, uci, forecasts) >>
              Env.current.scheduler.after(wait.millis) {
                Ok(Json.obj("reload" -> true))
              }
          }
        )
      }
    }
  }

  def help = Open { implicit ctx =>
    Ok(html.analyse.help(getBool("study"))).fuccess
  }
}
