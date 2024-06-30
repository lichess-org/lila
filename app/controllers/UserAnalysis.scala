package controllers

import shogi.format.forsyth.Sfen.SituationPlus
import shogi.format.forsyth.Sfen
import shogi.Situation
import shogi.variant.{ Standard, Variant }
import play.api.libs.json.Json
import play.api.mvc._
import scala.concurrent.duration._

import lila.api.Context
import lila.app._
import lila.game.Pov
import lila.round.Forecast.{ forecastJsonWriter, forecastStepJsonFormat }
import lila.round.JsonView.WithFlags
import lila.study.JsonView.tagsWrites
import views._

final class UserAnalysis(
    env: Env,
    gameC: => Game
) extends LilaController(env)
    with TheftPrevention {

  def index = load("", Standard)

  def parseArg(arg: String) =
    arg.split("/", 2) match {
      case Array(key) => load("", Variant orDefault key)
      case Array(key, sfen) =>
        Variant.byKey get key match {
          case Some(variant) => load(sfen, variant)
          case _             => load(arg, Standard)
        }
      case _ => load("", Standard)
    }

  def load(urlSfen: String, variant: Variant) =
    Open { implicit ctx =>
      val decodedSfen: Option[Sfen] = lila.common.String
        .decodeUriPath(urlSfen)
        .filter(_.trim.nonEmpty)
        .orElse(get("sfen")) map Sfen.clean
      val pov         = makePov(decodedSfen, variant)
      val orientation = get("color").flatMap(shogi.Color.fromName) | pov.color
      env.api.roundApi
        .userAnalysisJson(
          pov,
          ctx.pref,
          orientation,
          owner = false,
          me = ctx.me
        ) map { data =>
        Ok(html.board.userAnalysis(data, pov)).enableSharedArrayBuffer
      }
    }

  private[controllers] def makePov(sfen: Option[Sfen], variant: Variant, imported: Boolean = false): Pov = {
    val sitFrom = sfen.filter(_.value.nonEmpty).flatMap {
      _.toSituationPlus(variant)
    } | SituationPlus(Situation(variant), 1)
    makePov(sitFrom, imported)
  }

  private[controllers] def makePov(from: SituationPlus, imported: Boolean): Pov =
    Pov(
      lila.game.Game
        .make(
          shogi = shogi.Game(
            situation = from.situation,
            plies = from.plies,
            startedAtPly = from.plies,
            startedAtStep = from.stepNumber
          ),
          initialSfen = Some(from.toSfen),
          sentePlayer = lila.game.Player.make(shogi.Sente),
          gotePlayer = lila.game.Player.make(shogi.Gote),
          mode = shogi.Mode.Casual,
          source = if (imported) lila.game.Source.Import else lila.game.Source.Api,
          notationImport = None
        )
        .withId("synthetic"),
      from.situation.color
    )

  def game(id: String, color: String) =
    Open { implicit ctx =>
      OptionFuResult(env.game.gameRepo game id) { g =>
        env.round.proxyRepo upgradeIfPresent g flatMap { game =>
          val pov = Pov(game, shogi.Color.fromSente(color == "sente"))
          negotiate(
            html =
              if (game.replayable) Redirect(routes.Round.watcher(game.id, color)).fuccess
              else {
                val owner = isMyPov(pov)
                for {
                  data <-
                    env.api.roundApi
                      .userAnalysisJson(pov, ctx.pref, pov.color, owner = owner, me = ctx.me)
                } yield Ok(
                  html.board
                    .userAnalysis(
                      data,
                      pov,
                      withForecast = owner && !pov.game.synthetic && pov.game.playable
                    )
                ).noCache
              },
            api = apiVersion => mobileAnalysis(pov, apiVersion)
          )
        }
      }
    }

  private def mobileAnalysis(pov: Pov, apiVersion: lila.common.ApiVersion)(implicit
      ctx: Context
  ): Fu[Result] =
    gameC.preloadUsers(pov.game) zip
      env.analyse.analyser.get(pov.game) zip
      env.game.crosstableApi(pov.game) flatMap { case ((_, analysis), crosstable) =>
        import lila.game.JsonView.crosstableWrites
        env.api.roundApi.review(
          pov,
          apiVersion,
          tv = none,
          analysis,
          withFlags = WithFlags(division = true, clocks = true, movetimes = true)
        ) map { data =>
          Ok(data.add("crosstable", crosstable))
        }
      }

  // XHR only
  def notation =
    OpenBody { implicit ctx =>
      implicit val req = ctx.body
      lila.study.StudyForm.importFree.form
        .bindFromRequest()
        .fold(
          jsonFormError,
          data =>
            lila.study.NotationImport
              .userAnalysis(data.notation)
              .fold(
                err => BadRequest(err).fuccess,
                { case (game, root, tags) =>
                  val pov = Pov(game, shogi.Sente)
                  val baseData = env.round.jsonView
                    .userAnalysisJson(
                      pov,
                      ctx.pref,
                      pov.color,
                      owner = false,
                      me = ctx.me
                    )
                  Ok(
                    baseData ++ Json.obj(
                      "treeParts" -> lila.study.JsonView.partitionTreeJsonWriter.writes(
                        root
                      ),
                      "tags" -> tags
                    )
                  ).fuccess
                }
              )
        )
        .map(_ as JSON)
    }

  def forecasts(fullId: String) =
    AuthBody(parse.json) { implicit ctx => _ =>
      import lila.round.Forecast
      OptionFuResult(env.round.proxyRepo pov fullId) { pov =>
        if (isTheft(pov)) fuccess(theftResponse)
        else
          ctx.body.body
            .validate[Forecast.Steps]
            .fold(
              err => BadRequest(err.toString).fuccess,
              forecasts =>
                env.round.forecastApi.save(pov, forecasts) >>
                  env.round.forecastApi.loadForDisplay(pov) map {
                    case None     => Ok(Json.obj("none" -> true))
                    case Some(fc) => Ok(Json toJson fc) as JSON
                  } recover { case Forecast.OutOfSync =>
                    Ok(Json.obj("reload" -> true))
                  }
            )
      }
    }

  def forecastsOnMyTurn(fullId: String, usi: String) =
    AuthBody(parse.json) { implicit ctx => _ =>
      import lila.round.Forecast
      OptionFuResult(env.round.proxyRepo pov fullId) { pov =>
        if (isTheft(pov)) fuccess(theftResponse)
        else
          ctx.body.body
            .validate[Forecast.Steps]
            .fold(
              err => BadRequest(err.toString).fuccess,
              forecasts => {
                val wait = 50 + (Forecast maxPlies forecasts min 10) * 50
                env.round.forecastApi.playAndSave(pov, usi, forecasts) >>
                  lila.common.Future.sleep(wait.millis) inject
                  Ok(Json.obj("reload" -> true))
              }
            )
      }
    }

  def help =
    Open { implicit ctx =>
      Ok(html.analyse.help(getBool("study"))).fuccess
    }
}
