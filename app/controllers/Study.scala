package controllers

import play.api.http.ContentTypes
import play.api.i18n.Messages.Implicits._
import play.api.libs.json._
import play.api.mvc._
import scala.concurrent.duration._

import lila.app._
import views._

object Study extends LilaController {

  private def env = Env.study

  def show(id: String) = Open { implicit ctx =>
    val query = get("chapterId").fold(env.api byIdWithChapter id) { chapterId =>
      env.api.byIdWithChapter(id, chapterId)
    }
    OptionFuResult(query) {
      case lila.study.Study.WithChapter(study, chapter) => CanViewResult(study) {
        env.chapterRepo.orderedMetadataByStudy(study.id) flatMap { chapters =>
          val setup = chapter.setup
          val initialFen = chapter.root.fen
          val pov = UserAnalysis.makePov(initialFen.value.some, setup.variant)
          Env.round.jsonView.userAnalysisJson(pov, ctx.pref, setup.orientation, owner = false) zip
            Env.chat.api.userChat.find(study.id) zip
            env.version(id) flatMap {
              case ((baseData, chat), sVersion) =>
                import lila.socket.tree.Node.nodeJsonWriter
                val analysis = baseData ++ Json.obj(
                  "tree" -> lila.study.TreeBuilder(chapter.root))
                val data = lila.study.JsonView.JsData(
                  study = lila.study.JsonView(study, chapters),
                  analysis = analysis,
                  chat = lila.chat.JsonView(chat))
                negotiate(
                  html = Ok(html.study.show(study, data, sVersion)).fuccess,
                  api = _ => Ok(Json.obj(
                    "study" -> data.study,
                    "analysis" -> data.analysis)).fuccess
                )
            }
        }
      }
    } map NoCache
  }

  def websocket(id: String, apiVersion: Int) = SocketOption[JsValue] { implicit ctx =>
    get("sri") ?? { uid =>
      env.api byId id flatMap {
        _.filter(canView) ?? { study =>
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

  def pgn(id: String) = Open { implicit ctx =>
    OptionFuResult(env.api byId id) { study =>
      CanViewResult(study) {
        env.pgnDump(study) map { pgns =>
          Ok(pgns.mkString("\n\n\n")).withHeaders(
            CONTENT_TYPE -> ContentTypes.TEXT,
            CONTENT_DISPOSITION -> ("attachment; filename=" + (env.pgnDump filename study)))
        }
      }
    }
  }

  private def CanViewResult(study: lila.study.Study)(f: => Fu[Result])(implicit ctx: lila.api.Context) =
    if (canView(study)) f
    else fuccess(Unauthorized(html.study.restricted(study)))

  private def canView(study: lila.study.Study)(implicit ctx: lila.api.Context) =
    study.visibility == lila.study.Study.Visibility.Public ||
      ctx.userId.exists(study.members.contains)
}
