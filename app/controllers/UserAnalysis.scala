package controllers

import chess.format.Forsyth
import chess.format.Forsyth.SituationPlus
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
    val decodedFen = lila.common.String.decodeUriPath(urlFen)
      .map(_.replace("_", " ").trim).filter(_.nonEmpty)
      .orElse(get("fen"))
    val pov = makePov(decodedFen, variant)
    val orientation = get("color").flatMap(chess.Color.apply) | pov.color
    Env.api.roundApi.userAnalysisJson(pov, ctx.pref, decodedFen, orientation, owner = false, me = ctx.me) map { data =>
      Ok(html.board.userAnalysis(data, pov))
    }
  }

  private[controllers] def makePov(fen: Option[String], variant: Variant): Pov = makePov {
    fen.filter(_.nonEmpty).flatMap {
      Forsyth.<<<@(variant, _)
    } | SituationPlus(Situation(variant), 1)
  }

  private[controllers] def makePov(from: SituationPlus): Pov = Pov(
    lila.game.Game.make(
      game = chess.Game(
        situation = from.situation,
        turns = from.turns
      ),
      whitePlayer = lila.game.Player.white,
      blackPlayer = lila.game.Player.black,
      mode = chess.Mode.Casual,
      variant = from.situation.board.variant,
      source = lila.game.Source.Api,
      pgnImport = None
    ).copy(id = "synthetic"),
    from.situation.color
  )

  def game(id: String, color: String) = Open { implicit ctx =>
    OptionFuResult(GameRepo game id) { game =>
      val pov = Pov(game, chess.Color(color == "white"))
      if (game.replayable) negotiate(
        html = fuccess(Redirect(routes.Round.watcher(game.id, color))),
        api = apiVersion => mobileAnalysis(pov, apiVersion)
      )
      else GameRepo initialFen game.id flatMap { initialFen =>
        Env.api.roundApi.userAnalysisJson(pov, ctx.pref, initialFen, pov.color, owner = isMyPov(pov), me = ctx.me) flatMap { data =>
          negotiate(
            html = Ok(html.board.userAnalysis(data, pov)).fuccess,
            api = _ => Ok(data).fuccess
          )
        }
      } map NoCache
    }
  }

  private def mobileAnalysis(pov: Pov, apiVersion: lila.common.ApiVersion)(implicit ctx: Context): Fu[Result] =
    GameRepo initialFen pov.game.id flatMap { initialFen =>
      Game.preloadUsers(pov.game) zip
        (Env.analyse.analyser get pov.game.id) zip
        Env.game.crosstableApi.withMatchup(pov.game) zip
        Env.bookmark.api.exists(pov.game, ctx.me) flatMap {
          // case _ ~ analysis ~ analysisInProgress ~ simul ~ chat ~ crosstable ~ bookmarked ~ pgn =>
          case _ ~ analysis ~ crosstable ~ bookmarked =>
            import lila.game.JsonView.crosstableWithMatchupWrites
            Env.api.roundApi.review(pov, apiVersion,
              tv = none,
              analysis,
              initialFenO = initialFen.some,
              withFlags = WithFlags(division = true, opening = true, clocks = true, movetimes = true)) map { data =>
                Ok(data.add("crosstable", crosstable))
              }
        }
    }

  def socket = SocketOption { implicit ctx =>
    getSocketUid("sri") ?? { uid =>
      Env.analyse.socketHandler.join(uid, ctx.me) map some
    }
  }

  // XHR only
  def pgn = OpenBody { implicit ctx =>
    implicit val req = ctx.body
    Env.importer.forms.importForm.bindFromRequest.fold(
      failure => BadRequest(errorsAsJson(failure)).fuccess,
      data => Env.importer.importer.inMemory(data).fold(
        err => BadRequest(jsonError(err.shows)).fuccess, {
          case (game, fen) =>
            val pov = Pov(game, chess.White)
            Env.api.roundApi.userAnalysisJson(pov, ctx.pref, initialFen = fen.map(_.value), pov.color, owner = false, me = ctx.me) map { data =>
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
