package controllers

import play.api.http.ContentTypes
import play.api.i18n.Messages.Implicits._
import play.api.libs.json._
import play.api.mvc._
import scala.concurrent.duration._

import lila.api.Context
import lila.app._
import lila.common.HTTPRequest
import lila.study.Order
import views._

object Study extends LilaController {

  type ListUrl = String => Call

  private def env = Env.study

  def search(text: String, page: Int) = OpenBody { implicit ctx =>
    Reasonable(page) {
      if (text.trim.isEmpty)
        env.pager.all(ctx.me, Order.default, page) map { pag =>
          Ok(html.study.all(pag, Order.default))
        }
      else Env.studySearch(ctx.me)(text, page) map { pag =>
        Ok(html.study.search(pag, text))
      }
    }
  }

  def allDefault(page: Int) = all(Order.Hot.key, page)

  def all(o: String, page: Int) = Open { implicit ctx =>
    Reasonable(page) {
      Order(o) match {
        case Order.Oldest => Redirect(routes.Study.allDefault(page)).fuccess
        case order =>
          env.pager.all(ctx.me, order, page) map { pag =>
            Ok(html.study.all(pag, order))
          }
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

  def mine(order: String, page: Int) = Auth { implicit ctx => me =>
    env.pager.mine(me, Order(order), page) map { pag =>
      Ok(html.study.mine(pag, Order(order), me))
    }
  }

  def minePublic(order: String, page: Int) = Auth { implicit ctx => me =>
    env.pager.minePublic(me, Order(order), page) map { pag =>
      Ok(html.study.minePublic(pag, Order(order), me))
    }
  }

  def minePrivate(order: String, page: Int) = Auth { implicit ctx => me =>
    env.pager.minePrivate(me, Order(order), page) map { pag =>
      Ok(html.study.minePrivate(pag, Order(order), me))
    }
  }

  def mineMember(order: String, page: Int) = Auth { implicit ctx => me =>
    env.pager.mineMember(me, Order(order), page) map { pag =>
      Ok(html.study.mineMember(pag, Order(order), me))
    }
  }

  def mineLikes(order: String, page: Int) = Auth { implicit ctx => me =>
    env.pager.mineLikes(me, Order(order), page) map { pag =>
      Ok(html.study.mineLikes(pag, Order(order), me))
    }
  }

  def show(id: String) = Open { implicit ctx =>
    // chapterId is only specified when the user is desynced
    val query = get("chapterId").fold(env.api byIdWithChapter id) { chapterId =>
      env.api.byIdWithChapter(id, chapterId)
    }
    OptionFuResult(query) {
      case lila.study.Study.WithChapter(study, chapter) => CanViewResult(study) {
        env.chapterRepo.orderedMetadataByStudy(study.id) flatMap { chapters =>
          env.api.resetIfOld(study, chapters) flatMap { study =>
            if (HTTPRequest isSynchronousHttp ctx.req) env.studyRepo.incViews(study)
            val setup = chapter.setup
            val pov = UserAnalysis.makePov(chapter.root.fen.value.some, setup.variant)
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
      }
    } map NoCache
  }

  private def chatOf(study: lila.study.Study)(implicit ctx: lila.api.Context) =
    ctx.noKid ?? Env.chat.api.userChat.findMine(study.id, ctx.me).map(some)

  def chapter(id: String, chapterId: String) = Open { implicit ctx =>
    negotiate(
      html = fuccess(Redirect(s"${routes.Study.show(id)}#$chapterId")),
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

  def create = AuthBody { implicit ctx => me =>
    implicit val req = ctx.body
    lila.study.DataForm.form.bindFromRequest.fold(
      err => Redirect(routes.Study.byOwnerDefault(me.username)).fuccess,
      data => env.api.create(data, me) map { sc =>
        Redirect(routes.Study.show(sc.study.id))
      })
  }

  def delete(id: String) = Auth { implicit ctx => me =>
    env.api.byId(id) flatMap { study =>
      study.filter(_ isOwner me.id) ?? env.api.delete
    } inject Redirect(routes.Study.allDefault(1))
  }

  def embed(id: String, chapterId: String) = Open { implicit ctx =>
    env.api.byIdWithChapter(id, chapterId) flatMap {
      _.fold(embedNotFound) {
        case lila.study.Study.WithChapter(s, chapter) => CanViewResult(s) {
          val study = s withChapter chapter // rewind to beginning of chapter
          val setup = chapter.setup
          val pov = UserAnalysis.makePov(chapter.root.fen.value.some, setup.variant)
          Env.round.jsonView.userAnalysisJson(pov, ctx.pref, setup.orientation, owner = false) zip
            env.jsonView(study, List(chapter.metadata), chapter, ctx.me) flatMap {
              case (baseData, studyJson) =>
                import lila.socket.tree.Node.partitionTreeJsonWriter
                val analysis = baseData ++ Json.obj(
                  "treeParts" -> partitionTreeJsonWriter.writes(lila.study.TreeBuilder(chapter.root)))
                val data = lila.study.JsonView.JsData(
                  study = studyJson,
                  analysis = analysis)
                negotiate(
                  html = Ok(html.study.embed(study, chapter, data)).fuccess,
                  api = _ => Ok(Json.obj(
                  "study" -> data.study,
                  "analysis" -> data.analysis)).fuccess
                )
            }
        }
      }
    } map NoCache
  }

  private def embedNotFound(implicit ctx: Context): Fu[Result] =
    fuccess(NotFound(html.study.embedNotFound()))

  def cloneStudy(id: String) = Auth { implicit ctx => me =>
    OptionFuResult(env.api.byId(id)) { study =>
      CanViewResult(study) {
        Ok(html.study.clone(study)).fuccess
      }
    }
  }

  private val CloneLimitPerUser = new lila.memo.RateLimit(
    credits = 10,
    duration = 24 hour,
    name = "clone study per user",
    key = "clone_study.user")

  private val CloneLimitPerIP = new lila.memo.RateLimit(
    credits = 20,
    duration = 24 hour,
    name = "clone study per IP",
    key = "clone_study.ip")

  def cloneApply(id: String) = Auth { implicit ctx => me =>
    implicit val default = ornicar.scalalib.Zero.instance[Fu[Result]](notFound)
    CloneLimitPerUser(me.id, cost = 1) {
      CloneLimitPerIP(HTTPRequest lastRemoteAddress ctx.req, cost = 1) {
        OptionFuResult(env.api.byId(id)) { prev =>
          CanViewResult(prev) {
            env.api.clone(me, prev) map { study =>
              Redirect(routes.Study.show((study | prev).id))
            }
          }
        }
      }
    }
  }

  private val PgnRateLimitGlobal = new lila.memo.RateLimit(
    credits = 30,
    duration = 1 minute,
    name = "export study PGN global",
    key = "export.study_pgn.global")

  def pgn(id: String) = Open { implicit ctx =>
    OnlyHumans {
      PgnRateLimitGlobal("-", msg = HTTPRequest lastRemoteAddress ctx.req) {
        OptionFuResult(env.api byId id) { study =>
          CanViewResult(study) {
            lila.mon.export.pgn.study()
            env.pgnDump(study) map { pgns =>
              Ok(pgns.mkString("\n\n\n")).withHeaders(
                CONTENT_TYPE -> pgnContentType,
                CONTENT_DISPOSITION -> ("attachment; filename=" + (env.pgnDump filename study)))
            }
          }
        }
      }
    }
  }

  def chapterPgn(id: String, chapterId: String) = Open { implicit ctx =>
    OnlyHumans {
      env.api.byIdWithChapter(id, chapterId) flatMap {
        _.fold(notFound) {
          case lila.study.Study.WithChapter(study, chapter) => CanViewResult(study) {
            lila.mon.export.pgn.studyChapter()
            env.pgnDump.ofChapter(study, chapter) map { pgn =>
              Ok(pgn.toString).withHeaders(
                CONTENT_TYPE -> pgnContentType,
                CONTENT_DISPOSITION -> ("attachment; filename=" + (env.pgnDump.filename(study, chapter))))
            }
          }
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
