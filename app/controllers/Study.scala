package controllers

import chess.variant.Variant
import play.api.i18n.Messages.Implicits._
import play.api.libs.json._
import play.api.mvc._
import scala.concurrent.duration._

import lila.app._
import views._

object Study extends LilaController {

  private def env = Env.study

  def show(id: String) = Open { implicit ctx =>
    OptionFuResult(env.api byIdWithChapter id) {
      case lila.study.Study.WithChapter(study, chapter) =>
        val setup = chapter.setup
        val initialFen = chapter.root.fen
        val pov = UserAnalysis.makePov(initialFen.value.some, setup.variant)
        Env.round.jsonView.userAnalysisJson(pov, ctx.pref, setup.orientation, owner = false) zip
          Env.chat.api.userChat.find(study.id) zip
          env.version(id) map {
            case ((baseData, chat), sVersion) =>
              import lila.socket.tree.Node.nodeJsonWriter
              val analysis = baseData ++ Json.obj(
                "tree" -> lila.study.TreeBuilder(chapter.root))
              val data = lila.study.JsonView.JsData(
                study = lila.study.JsonView.study(study),
                analysis = analysis,
                chat = lila.chat.JsonView(chat))
              Ok(html.study.show(study, data, sVersion))
          }
    } map NoCache
  }

  def websocket(id: String, apiVersion: Int) = SocketOption[JsValue] { implicit ctx =>
    get("sri") ?? { uid =>
      env.api byId id flatMap {
        _ ?? { study =>
          env.socketHandler.join(
            studyId = id,
            uid = lila.socket.Socket.Uid(uid),
            user = ctx.me,
            owner = ctx.userId.exists(study.isOwner))
        }
      }
    }
  }

  def create = AuthBody { implicit ctx =>
    me =>
      env.api.create(me) map { sc =>
        Redirect(routes.Study.show(sc.study.id))
      }
  }
}
