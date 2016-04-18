package controllers

import chess.variant.Variant
import play.api.i18n.Messages.Implicits._
import play.api.libs.json.JsValue
import play.api.mvc._
import scala.concurrent.duration._

import lila.app._
import views._

object Study extends LilaController {

  private def env = Env.study

  def show(id: String) = Open { implicit ctx =>
    OptionFuResult(env.api byId id) { study =>
      study.firstChapter ?? { chapter =>
        val setup = chapter.setup
        val initialFen = setup.initialFen
        val pov = UserAnalysis.makePov(initialFen.value.some, setup.variant)
        Env.api.roundApi.freeStudyJson(pov, ctx.pref, initialFen.value.some, setup.orientation) zip
          env.version(id) map {
            case (analysisJson, version) =>
              val data = lila.study.JsonView.BiData(
                study = env.jsonView.study(study),
                analysis = analysisJson)
              Ok(html.study.show(study, data, version))
          }
      }
    } map NoCache
  }

  def websocket(id: String, apiVersion: Int) = SocketOption[JsValue] { implicit ctx =>
    get("sri") ?? { uid =>
      env.api byId id flatMap {
        _ ?? { study =>
          env.socketHandler.join(
            studyId = id,
            uid = uid,
            userId = ctx.userId,
            owner = ctx.userId.contains(study.owner))
        }
      }
    }
  }

  def create = AuthBody { implicit ctx =>
    me =>
      env.api.create(me) map { study =>
        Redirect(routes.Study.show(study.id))
      }
  }
}
