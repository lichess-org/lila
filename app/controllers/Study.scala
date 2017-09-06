package controllers

import play.api.libs.json._
import play.api.mvc._
import scala.concurrent.duration._

import lila.api.Context
import lila.app._
import lila.common.{ IpAddress, HTTPRequest }
import lila.study.Study.WithChapter
import lila.study.{ Chapter, Order, Study => StudyModel }
import lila.chat.Chat
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

  private def showQuery(query: Fu[Option[WithChapter]])(implicit ctx: Context) =
    OptionFuResult(query) {
      case WithChapter(s, c) => CanViewResult(s) {
        import lila.tree.Node.partitionTreeJsonWriter
        for {
          chapters <- env.chapterRepo.orderedMetadataByStudy(s.id)
          (study, resetToChapter) <- env.api.resetIfOld(s, chapters)
          chapter = resetToChapter | c
          _ <- Env.user.lightUserApi preloadMany study.members.ids.toList
          _ = if (HTTPRequest isSynchronousHttp ctx.req) env.studyRepo.incViews(study)
          initialFen = chapter.root.fen.value.some
          pov = UserAnalysis.makePov(initialFen, chapter.setup.variant)
          baseData = Env.round.jsonView.userAnalysisJson(pov, ctx.pref, initialFen, chapter.setup.orientation, owner = false, me = ctx.me)
          studyJson <- env.jsonView(study, chapters, chapter, ctx.me)
          data = lila.study.JsonView.JsData(
            study = studyJson,
            analysis = baseData ++ Json.obj(
              "treeParts" -> partitionTreeJsonWriter.writes {
                lila.study.TreeBuilder(chapter.root, chapter.setup.variant)
              }
            )
          )
          res <- negotiate(
            html = for {
              chat <- chatOf(study)
              sVersion <- env.version(study.id)
            } yield Ok(html.study.show(study, data, chat, sVersion)),
            api = _ => Ok(Json.obj(
              "study" -> data.study,
              "analysis" -> data.analysis
            )).fuccess
          )
        } yield res
      }
    } map NoCache

  def show(id: String) = Open { implicit ctx =>
    showQuery(env.api byIdWithChapter id)
  }

  def chapter(id: String, chapterId: String) = Open { implicit ctx =>
    showQuery(env.api.byIdWithChapter(id, chapterId))
  }

  def chapterMeta(id: String, chapterId: String) = Open { implicit ctx =>
    env.chapterRepo.byId(chapterId).map {
      _.filter(_.studyId.value == id) ?? { chapter =>
        Ok(env.jsonView.chapterConfig(chapter))
      }
    }
  }

  private def chatOf(study: lila.study.Study)(implicit ctx: lila.api.Context) =
    ctx.noKid ?? Env.chat.api.userChat.findMine(Chat.Id(study.id.value), ctx.me).map(some)

  def websocket(id: String, apiVersion: Int) = SocketOption { implicit ctx =>
    get("sri") ?? { uid =>
      env.api byId id flatMap {
        _.filter(canView) ?? { study =>
          env.socketHandler.join(
            studyId = id,
            uid = lila.socket.Socket.Uid(uid),
            user = ctx.me
          )
        }
      }
    }
  }

  def createAs = AuthBody { implicit ctx => me =>
    implicit val req = ctx.body
    lila.study.DataForm.form.bindFromRequest.fold(
      err => Redirect(routes.Study.byOwnerDefault(me.username)).fuccess,
      data => for {
        owner <- env.studyRepo.recentByOwner(me.id, 50)
        contrib <- env.studyRepo.recentByContributor(me.id, 50)
        res <- if (owner.isEmpty && contrib.isEmpty) createStudy(data, me)
        else Ok(html.study.create(data, owner, contrib)).fuccess
      } yield res
    )
  }

  def create = AuthBody { implicit ctx => me =>
    implicit val req = ctx.body
    lila.study.DataForm.form.bindFromRequest.fold(
      err => Redirect(routes.Study.byOwnerDefault(me.username)).fuccess,
      data => createStudy(data, me)
    )
  }

  private def createStudy(data: lila.study.DataForm.Data, me: lila.user.User)(implicit ctx: Context) =
    env.api.create(data, me) flatMap {
      _.fold(notFound) { sc =>
        Redirect(routes.Study.show(sc.study.id.value)).fuccess
      }
    }

  def delete(id: String) = Auth { implicit ctx => me =>
    env.api.byIdAndOwner(id, me) flatMap {
      _ ?? env.api.delete
    } inject Redirect(routes.Study.mine("hot"))
  }

  def clearChat(id: String) = Auth { implicit ctx => me =>
    env.api.isOwner(id, me) flatMap {
      _ ?? Env.chat.api.userChat.clear(Chat.Id(id))
    } inject Redirect(routes.Study.show(id))
  }

  def embed(id: String, chapterId: String) = Open { implicit ctx =>
    env.api.byIdWithChapter(id, chapterId) flatMap {
      _.fold(embedNotFound) {
        case WithChapter(study, chapter) => CanViewResult(study) {
          env.jsonView(study.copy(
            members = lila.study.StudyMembers(Map.empty) // don't need no members
          ), List(chapter.metadata), chapter, ctx.me) flatMap { studyJson =>
            val setup = chapter.setup
            val initialFen = chapter.root.fen.value.some
            val pov = UserAnalysis.makePov(initialFen, setup.variant)
            val baseData = Env.round.jsonView.userAnalysisJson(pov, ctx.pref, initialFen, setup.orientation, owner = false, me = ctx.me)
            import lila.tree.Node.partitionTreeJsonWriter
            val analysis = baseData ++ Json.obj(
              "treeParts" -> partitionTreeJsonWriter.writes {
                lila.study.TreeBuilder.makeRoot(chapter.root)
              }
            )
            val data = lila.study.JsonView.JsData(
              study = studyJson,
              analysis = analysis
            )
            negotiate(
              html = Ok(html.study.embed(study, chapter, data)).fuccess,
              api = _ => Ok(Json.obj("study" -> data.study, "analysis" -> data.analysis)).fuccess
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

  private val CloneLimitPerUser = new lila.memo.RateLimit[lila.user.User.ID](
    credits = 10 * 3,
    duration = 24 hour,
    name = "clone study per user",
    key = "clone_study.user"
  )

  private val CloneLimitPerIP = new lila.memo.RateLimit[IpAddress](
    credits = 20 * 3,
    duration = 24 hour,
    name = "clone study per IP",
    key = "clone_study.ip"
  )

  def cloneApply(id: String) = Auth { implicit ctx => me =>
    implicit val default = ornicar.scalalib.Zero.instance[Fu[Result]](notFound)
    val cost = if (isGranted(_.Coach) || me.hasTitle) 1 else 3
    CloneLimitPerUser(me.id, cost = cost) {
      CloneLimitPerIP(HTTPRequest lastRemoteAddress ctx.req, cost = cost) {
        OptionFuResult(env.api.byId(id)) { prev =>
          CanViewResult(prev) {
            env.api.clone(me, prev) map { study =>
              Redirect(routes.Study.show((study | prev).id.value))
            }
          }
        }
      }
    }
  }

  private val PgnRateLimitGlobal = new lila.memo.RateLimit[String](
    credits = 30,
    duration = 1 minute,
    name = "export study PGN global",
    key = "export.study_pgn.global"
  )

  def pgn(id: String) = Open { implicit ctx =>
    OnlyHumans {
      PgnRateLimitGlobal("-", msg = HTTPRequest.lastRemoteAddress(ctx.req).value) {
        OptionFuResult(env.api byId id) { study =>
          CanViewResult(study) {
            lila.mon.export.pgn.study()
            env.pgnDump(study) map { pgns =>
              Ok(pgns.mkString("\n\n\n")).withHeaders(
                CONTENT_DISPOSITION -> ("attachment; filename=" + (env.pgnDump filename study))
              ) as pgnContentType
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
          case WithChapter(study, chapter) => CanViewResult(study) {
            lila.mon.export.pgn.studyChapter()
            Ok(env.pgnDump.ofChapter(study, chapter).toString).withHeaders(
              CONTENT_DISPOSITION -> ("attachment; filename=" + (env.pgnDump.filename(study, chapter)))
            ).as(pgnContentType).fuccess
          }
        }
      }
    }
  }

  private def CanViewResult(study: StudyModel)(f: => Fu[Result])(implicit ctx: lila.api.Context) =
    if (canView(study)) f
    else negotiate(
      html = fuccess(Unauthorized(html.study.restricted(study))),
      api = _ => fuccess(Unauthorized(jsonError("This study is now private")))
    )

  private def canView(study: StudyModel)(implicit ctx: lila.api.Context) =
    !study.isPrivate || ctx.userId.exists(study.members.contains)

  private implicit def makeStudyId(id: String): StudyModel.Id = StudyModel.Id(id)
  private implicit def makeChapterId(id: String): Chapter.Id = Chapter.Id(id)
}
