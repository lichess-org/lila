package controllers

import play.api.http.ContentTypes
import play.api.i18n.Messages.Implicits._
import play.api.libs.json._
import play.api.mvc._
import scala.concurrent.duration._

import lila.app._
import lila.common.HTTPRequest
import lila.study.Order
import views._

object Study extends LilaController {

  type ListUrl = String => Call

  private def env = Env.study

  def search(text: String, page: Int) = OpenBody { implicit ctx =>
    if (text.trim.isEmpty)
      env.pager.all(ctx.me, Order.default, page) map { pag =>
        Ok(html.study.all(pag, Order.default))
      }
      else Env.studySearch(ctx.me)(text, page) map { pag =>
        Ok(html.study.search(pag, text))
      }
  }

  def allDefault(page: Int) = all(Order.Hot.key, page)

  def all(o: String, page: Int) = Open { implicit ctx =>
    Order(o) match {
      case Order.Oldest => Redirect(routes.Study.allDefault(page)).fuccess
      case order =>
        env.pager.all(ctx.me, order, page) map { pag =>
          Ok(html.study.all(pag, order))
        }
    }
  }

  def byOwnerDefault(username: String, page: Int) = byOwner(username, Order.default.key, page)

  def byOwner(username: String, order: String, page: Int) = Open { implicit ctx =>
    OptionFuOk(lila.user.UserRepo named username) { owner =>
      env.pager.byOwner(owner, ctx.me, Order(order), page) map { pag =>
        html.study.byOwner(pag, Order(order), owner)
      }
    }
  }

  def mine(order: String, page: Int) = Auth { implicit ctx =>
    me =>
      env.pager.mine(me, Order(order), page) map { pag =>
        Ok(html.study.mine(pag, Order(order), me))
      }
  }

  def minePublic(order: String, page: Int) = Auth { implicit ctx =>
    me =>
      env.pager.minePublic(me, Order(order), page) map { pag =>
        Ok(html.study.minePublic(pag, Order(order), me))
      }
  }

  def minePrivate(order: String, page: Int) = Auth { implicit ctx =>
    me =>
      env.pager.minePrivate(me, Order(order), page) map { pag =>
        Ok(html.study.minePrivate(pag, Order(order), me))
      }
  }

  def mineMember(order: String, page: Int) = Auth { implicit ctx =>
    me =>
      env.pager.mineMember(me, Order(order), page) map { pag =>
        Ok(html.study.mineMember(pag, Order(order), me))
      }
  }

  def mineLikes(order: String, page: Int) = Auth { implicit ctx =>
    me =>
      env.pager.mineLikes(me, Order(order), page) map { pag =>
        Ok(html.study.mineLikes(pag, Order(order), me))
      }
  }

  def show(id: String) = Open { implicit ctx =>
    val query = get("chapterId").fold(env.api byIdWithChapter id) { chapterId =>
      env.api.byIdWithChapter(id, chapterId)
    }
    OptionFuResult(query) {
      case lila.study.Study.WithChapter(study, chapter) => CanViewResult(study) {
        env.chapterRepo.orderedMetadataByStudy(study.id) flatMap { chapters =>
          if (HTTPRequest isSynchronousHttp ctx.req) env.studyRepo.incViews(study)
          val setup = chapter.setup
          val initialFen = chapter.root.fen
          val pov = UserAnalysis.makePov(initialFen.value.some, setup.variant)
          Env.round.jsonView.userAnalysisJson(pov, ctx.pref, setup.orientation, owner = false) zip
            chatOf(study) zip
            env.jsonView(study, chapters, chapter, ctx.me) zip
            env.version(id) flatMap {
              case (((baseData, chat), studyJson), sVersion) =>
                import lila.socket.tree.Node.partitionTreeJsonWriter
                val analysis = baseData ++ Json.obj(
                  "treeParts" -> partitionTreeJsonWriter.writes(lila.study.TreeBuilder(chapter.root)))
                val data = lila.study.JsonView.JsData(
                  study = studyJson,
                  analysis = analysis)
                negotiate(
                  html = Ok(html.study.show(study, data, chat, sVersion)).fuccess,
                  api = _ => Ok(Json.obj(
                    "study" -> data.study,
                    "analysis" -> data.analysis)).fuccess
                )
            }
        }
      }
    } map NoCache
  }

  private def chatOf(study: lila.study.Study)(implicit ctx: lila.api.Context) =
    ctx.noKid ?? Env.chat.api.userChat.findMine(study.id, ctx.me).map(some)

  def chapter(id: String, chapterId: String) = Open { implicit ctx =>
    negotiate(
      html = notFound,
      api = _ => env.chapterRepo.byId(chapterId).map {
        _.filter(_.studyId == id) ?? { chapter =>
          Ok(env.jsonView.chapterConfig(chapter))
        }
      })
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
      implicit val req = ctx.body
      lila.study.DataForm.form.bindFromRequest.fold(
        err => Redirect(routes.Study.byOwnerDefault(me.username)).fuccess,
        data => env.api.create(data, me) map { sc =>
          Redirect(routes.Study.show(sc.study.id))
        })
  }

  def delete(id: String) = Auth { implicit ctx =>
    me =>
      env.api.byId(id) flatMap { study =>
        study.filter(_ isOwner me.id) ?? env.api.delete
      } inject Redirect(routes.Study.allDefault(1))
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
    study.isPublic || ctx.userId.exists(study.members.contains)
}
