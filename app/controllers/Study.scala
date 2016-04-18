package controllers

import chess.variant.Variant
import play.api.i18n.Messages.Implicits._
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
        Env.api.roundApi.freeStudyJson(pov, ctx.pref, initialFen.value.some, setup.orientation) map { analysisJson =>
          val data = lila.study.JsonView.BiData(
            study = env.jsonView.study(study),
            analysis = analysisJson
          )
          Ok(html.study.show(study, data))
        }
      }
    } map NoCache
  }

  def create = AuthBody { implicit ctx =>
    me =>
      env.api.create(me) map { study =>
        Redirect(routes.Study.show(study.id))
      }
  }
}
