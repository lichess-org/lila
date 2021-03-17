package controllers

import play.api.libs.json._
import play.api.mvc._
import scala.concurrent.duration._

import lila.api.Context
import lila.app._
import lila.chat.Chat
import lila.common.paginator.{ Paginator, PaginatorJson }
import lila.common.{ HTTPRequest, IpAddress }
import lila.study.actorApi.Who
import lila.study.JsonView.JsData
import lila.study.Study.WithChapter
import lila.study.{ Chapter, Order, Study => StudyModel }
import lila.tree.Node.partitionTreeJsonWriter
import views._

final class Study(
    env: Env,
    userAnalysisC: => UserAnalysis,
    apiC: => Api
) extends LilaController(env) {

  def search(text: String, page: Int) =
    OpenBody { implicit ctx =>
      Reasonable(page) {
        if (text.trim.isEmpty)
          env.study.pager.all(ctx.me, Order.default, page) flatMap { pag =>
            negotiate(
              html = Ok(html.study.list.all(pag, Order.default)).fuccess,
              api = _ => apiStudies(pag)
            )
          }
        else
          env.studySearch(ctx.me)(text, page) flatMap { pag =>
            negotiate(
              html = Ok(html.study.list.search(pag, text)).fuccess,
              api = _ => apiStudies(pag)
            )
          }
      }
    }

  def allDefault(page: Int) = all(Order.Hot.key, page)

  def all(o: String, page: Int) =
    Open { implicit ctx =>
      Reasonable(page) {
        Order(o) match {
          case Order.Oldest => Redirect(routes.Study.allDefault(page)).fuccess
          case order =>
            env.study.pager.all(ctx.me, order, page) flatMap { pag =>
              negotiate(
                html = Ok(html.study.list.all(pag, order)).fuccess,
                api = _ => apiStudies(pag)
              )
            }
        }
      }
    }

  def byOwnerDefault(username: String, page: Int) = byOwner(username, Order.default.key, page)

  def byOwner(username: String, order: String, page: Int) =
    Open { implicit ctx =>
      env.user.repo.named(username).flatMap {
        _.fold(notFound(ctx)) { owner =>
          env.study.pager.byOwner(owner, ctx.me, Order(order), page) flatMap { pag =>
            negotiate(
              html = Ok(html.study.list.byOwner(pag, Order(order), owner)).fuccess,
              api = _ => apiStudies(pag)
            )
          }
        }
      }
    }

  def mine(order: String, page: Int) =
    Auth { implicit ctx => me =>
      env.study.pager.mine(me, Order(order), page) flatMap { pag =>
        negotiate(
          html = env.study.topicApi.userTopics(me.id) map { topics =>
            Ok(html.study.list.mine(pag, Order(order), me, topics))
          },
          api = _ => apiStudies(pag)
        )
      }
    }

  def minePublic(order: String, page: Int) =
    Auth { implicit ctx => me =>
      env.study.pager.minePublic(me, Order(order), page) flatMap { pag =>
        negotiate(
          html = Ok(html.study.list.minePublic(pag, Order(order), me)).fuccess,
          api = _ => apiStudies(pag)
        )
      }
    }

  def minePrivate(order: String, page: Int) =
    Auth { implicit ctx => me =>
      env.study.pager.minePrivate(me, Order(order), page) flatMap { pag =>
        negotiate(
          html = Ok(html.study.list.minePrivate(pag, Order(order), me)).fuccess,
          api = _ => apiStudies(pag)
        )
      }
    }

  def mineMember(order: String, page: Int) =
    Auth { implicit ctx => me =>
      env.study.pager.mineMember(me, Order(order), page) flatMap { pag =>
        negotiate(
          html = env.study.topicApi.userTopics(me.id) map { topics =>
            Ok(html.study.list.mineMember(pag, Order(order), me, topics))
          },
          api = _ => apiStudies(pag)
        )
      }
    }

  def mineLikes(order: String, page: Int) =
    Auth { implicit ctx => me =>
      env.study.pager.mineLikes(me, Order(order), page) flatMap { pag =>
        negotiate(
          html = Ok(html.study.list.mineLikes(pag, Order(order))).fuccess,
          api = _ => apiStudies(pag)
        )
      }
    }

  def byTopic(name: String, order: String, page: Int) =
    Open { implicit ctx =>
      lila.study.StudyTopic fromStr name match {
        case None => notFound
        case Some(topic) =>
          env.study.pager.byTopic(topic, ctx.me, Order(order), page) zip
            ctx.me.??(u => env.study.topicApi.userTopics(u.id) dmap some) map { case (pag, topics) =>
              Ok(html.study.topic.show(topic, pag, Order(order), topics))
            }
      }
    }

  private def apiStudies(pager: Paginator[StudyModel.WithChaptersAndLiked]) = {
    implicit val pagerWriter = Writes[StudyModel.WithChaptersAndLiked] { s =>
      env.study.jsonView.pagerData(s)
    }
    Ok(
      Json.obj(
        "paginator" -> PaginatorJson(pager)
      )
    ).fuccess
  }

  private def orRelay(id: String, chapterId: Option[String] = None)(
      f: => Fu[Result]
  )(implicit ctx: Context): Fu[Result] =
    if (HTTPRequest isRedirectable ctx.req) env.relay.api.getOngoing(lila.relay.Relay.Id(id)) flatMap {
      _.fold(f) { relay =>
        fuccess(Redirect {
          chapterId.fold(routes.Relay.show(relay.slug, relay.id.value)) { c =>
            routes.Relay.chapter(relay.slug, relay.id.value, c)
          }
        })
      }
    }
    else f

  private def showQuery(query: Fu[Option[WithChapter]])(implicit ctx: Context): Fu[Result] =
    OptionFuResult(query) { oldSc =>
      CanViewResult(oldSc.study) {
        for {
          (sc, data) <- getJsonData(oldSc)
          res <- negotiate(
            html = for {
              chat     <- chatOf(sc.study)
              sVersion <- env.study.version(sc.study.id)
              streams  <- streamsOf(sc.study)
            } yield EnableSharedArrayBuffer(Ok(html.study.show(sc.study, data, chat, sVersion, streams))),
            api = _ =>
              chatOf(sc.study).map { chatOpt =>
                Ok(
                  Json.obj(
                    "study" -> data.study.add("chat" -> chatOpt.map { c =>
                      lila.chat.JsonView.mobile(
                        chat = c.chat,
                        writeable = ctx.userId.??(sc.study.canChat)
                      )
                    }),
                    "analysis" -> data.analysis
                  )
                )
              }
          )
        } yield res
      }
    } map NoCache

  private[controllers] def getJsonData(sc: WithChapter)(implicit ctx: Context): Fu[(WithChapter, JsData)] =
    for {
      chapters                <- env.study.chapterRepo.orderedMetadataByStudy(sc.study.id)
      (study, resetToChapter) <- env.study.api.resetIfOld(sc.study, chapters)
      chapter = resetToChapter | sc.chapter
      _ <- env.user.lightUserApi preloadMany study.members.ids.toList
      pov = userAnalysisC.makePov(chapter.root.fen.some, chapter.setup.variant)
      analysis <- chapter.serverEval.exists(_.done) ?? env.analyse.analyser.byId(chapter.id.value)
      division = analysis.isDefined option env.study.serverEvalMerger.divisionOf(chapter)
      baseData = env.round.jsonView.userAnalysisJson(
        pov,
        ctx.pref,
        chapter.root.fen.some,
        chapter.setup.orientation,
        owner = false,
        me = ctx.me,
        division = division
      )
      studyJson <- env.study.jsonView(study, chapters, chapter, ctx.me)
    } yield WithChapter(study, chapter) -> JsData(
      study = studyJson,
      analysis = baseData
        .add(
          "treeParts" -> partitionTreeJsonWriter.writes {
            lila.study.TreeBuilder(chapter.root, chapter.setup.variant)
          }.some
        )
        .add("analysis" -> analysis.map { lila.study.ServerEval.toJson(chapter, _) })
    )

  def show(id: String) =
    Open { implicit ctx =>
      orRelay(id) {
        showQuery(env.study.api byIdWithChapter id)
      }
    }

  def chapter(id: String, chapterId: String) =
    Open { implicit ctx =>
      orRelay(id, chapterId.some) {
        showQuery(env.study.api.byIdWithChapter(id, chapterId))
      }
    }

  def chapterMeta(id: String, chapterId: String) =
    Open { _ =>
      env.study.chapterRepo.byId(chapterId).map {
        _.filter(_.studyId.value == id) ?? { chapter =>
          Ok(env.study.jsonView.chapterConfig(chapter))
        }
      }
    }

  private[controllers] def chatOf(study: lila.study.Study)(implicit ctx: Context) = {
    !ctx.kid &&         // no public chats for kids
    ctx.me.fold(true) { // anon can see public chats
      env.chat.panic.allowed
    }
  } ?? env.chat.api.userChat
    .findMine(Chat.Id(study.id.value), ctx.me)
    .dmap(some)
    .mon(_.chat.fetch("study"))

  def createAs =
    AuthBody { implicit ctx => me =>
      implicit val req = ctx.body
      lila.study.StudyForm.importGame.form
        .bindFromRequest()
        .fold(
          _ => Redirect(routes.Study.byOwnerDefault(me.username)).fuccess,
          data =>
            for {
              owner   <- env.study.studyRepo.recentByOwner(me.id, 50)
              contrib <- env.study.studyRepo.recentByContributor(me.id, 50)
              res <-
                if (owner.isEmpty && contrib.isEmpty) createStudy(data, me)
                else Ok(html.study.create(data, owner, contrib)).fuccess
            } yield res
        )
    }

  def create =
    AuthBody { implicit ctx => me =>
      implicit val req = ctx.body
      lila.study.StudyForm.importGame.form
        .bindFromRequest()
        .fold(
          _ => Redirect(routes.Study.byOwnerDefault(me.username)).fuccess,
          data => createStudy(data, me)
        )
    }

  private def createStudy(data: lila.study.StudyForm.importGame.Data, me: lila.user.User)(implicit
      ctx: Context
  ) =
    env.study.api.importGame(lila.study.StudyMaker.ImportGame(data), me) flatMap {
      _.fold(notFound) { sc =>
        Redirect(routes.Study.show(sc.study.id.value)).fuccess
      }
    }

  def delete(id: String) =
    Auth { _ => me =>
      env.study.api.byIdAndOwner(id, me) flatMap {
        _ ?? env.study.api.delete
      } inject Redirect(routes.Study.mine("hot"))
    }

  def clearChat(id: String) =
    Auth { _ => me =>
      env.study.api.isOwner(id, me) flatMap {
        _ ?? env.chat.api.userChat.clear(Chat.Id(id))
      } inject Redirect(routes.Study.show(id))
    }

  def importPgn(id: String) =
    AuthBody { implicit ctx => me =>
      implicit val req = ctx.body
      get("sri") ?? { sri =>
        lila.study.StudyForm.importPgn.form
          .bindFromRequest()
          .fold(
            jsonFormError,
            data =>
              env.study.api.importPgns(
                StudyModel.Id(id),
                data.toChapterDatas,
                sticky = data.sticky
              )(Who(me.id, lila.socket.Socket.Sri(sri)))
          )
      }
    }

  def admin(id: String) =
    Secure(_.StudyAdmin) { _ => me =>
      env.study.api.adminInvite(id, me) inject Redirect(routes.Study.show(id))
    }

  def embed(id: String, chapterId: String) =
    Action.async { implicit req =>
      env.study.api.byIdWithChapter(id, chapterId).map(_.filterNot(_.study.isPrivate)) flatMap {
        _.fold(embedNotFound) { case WithChapter(study, chapter) =>
          for {
            chapters <- env.study.chapterRepo.idNames(study.id)
            studyJson <- env.study.jsonView(
              study.copy(
                members = lila.study.StudyMembers(Map.empty) // don't need no members
              ),
              List(chapter.metadata),
              chapter,
              none
            )
            setup      = chapter.setup
            initialFen = chapter.root.fen.some
            pov        = userAnalysisC.makePov(initialFen, setup.variant)
            baseData = env.round.jsonView.userAnalysisJson(
              pov,
              lila.pref.Pref.default,
              initialFen,
              setup.orientation,
              owner = false,
              me = none
            )
            analysis = baseData ++ Json.obj(
              "treeParts" -> partitionTreeJsonWriter.writes {
                lila.study.TreeBuilder.makeRoot(chapter.root, setup.variant)
              }
            )
            data = lila.study.JsonView.JsData(study = studyJson, analysis = analysis)
            result <- negotiate(
              html = Ok(html.study.embed(study, chapter, chapters, data)).fuccess,
              api = _ => Ok(Json.obj("study" -> data.study, "analysis" -> data.analysis)).fuccess
            )
          } yield result
        }
      } map NoCache
    }

  private def embedNotFound(implicit req: RequestHeader): Fu[Result] =
    fuccess(NotFound(html.study.embed.notFound))

  def cloneStudy(id: String) =
    Auth { implicit ctx => _ =>
      OptionFuResult(env.study.api.byId(id)) { study =>
        CanViewResult(study) {
          Ok(html.study.clone(study)).fuccess
        }
      }
    }

  private val CloneLimitPerUser = new lila.memo.RateLimit[lila.user.User.ID](
    credits = 10 * 3,
    duration = 24.hour,
    key = "study.clone.user"
  )

  private val CloneLimitPerIP = new lila.memo.RateLimit[IpAddress](
    credits = 20 * 3,
    duration = 24.hour,
    key = "study.clone.ip"
  )

  def cloneApply(id: String) =
    Auth { implicit ctx => me =>
      val cost = if (isGranted(_.Coach) || me.hasTitle) 1 else 3
      CloneLimitPerUser(me.id, cost = cost) {
        CloneLimitPerIP(HTTPRequest ipAddress ctx.req, cost = cost) {
          OptionFuResult(env.study.api.byId(id)) { prev =>
            CanViewResult(prev) {
              env.study.api.clone(me, prev) map { study =>
                Redirect(routes.Study.show((study | prev).id.value))
              }
            }
          }
        }(rateLimitedFu)
      }(rateLimitedFu)
    }

  private val PgnRateLimitPerIp = new lila.memo.RateLimit[IpAddress](
    credits = 30,
    duration = 1.minute,
    key = "export.study.pgn.ip"
  )

  def pgn(id: String) =
    Open { implicit ctx =>
      PgnRateLimitPerIp(HTTPRequest ipAddress ctx.req) {
        OptionFuResult(env.study.api byId id) { study =>
          CanViewResult(study) {
            lila.mon.export.pgn.study.increment()
            Ok.chunked(env.study.pgnDump(study, requestPgnFlags(ctx.req)))
              .withHeaders(
                noProxyBufferHeader,
                CONTENT_DISPOSITION -> s"attachment; filename=${env.study.pgnDump filename study}.pgn"
              )
              .as(pgnContentType)
              .fuccess
          }
        }
      }(rateLimitedFu)
    }

  def chapterPgn(id: String, chapterId: String) =
    Open { implicit ctx =>
      env.study.api.byIdWithChapter(id, chapterId) flatMap {
        _.fold(notFound) { case WithChapter(study, chapter) =>
          CanViewResult(study) {
            lila.mon.export.pgn.studyChapter.increment()
            Ok(env.study.pgnDump.ofChapter(study, requestPgnFlags(ctx.req))(chapter).toString)
              .withHeaders(
                CONTENT_DISPOSITION -> s"attachment; filename=${env.study.pgnDump.filename(study, chapter)}.pgn"
              )
              .as(pgnContentType)
              .fuccess
          }
        }
      }
    }

  def export(username: String) =
    Open { implicit ctx =>
      val userId = lila.user.User normalize username
      val flags  = requestPgnFlags(ctx.req)
      val isMe   = ctx.userId has userId
      apiC
        .GlobalConcurrencyLimitPerIpAndUserOption(ctx.req, ctx.me) {
          env.study.studyRepo
            .sourceByOwner(userId, isMe)
            .flatMapConcat(env.study.pgnDump(_, flags))
            .withAttributes(
              akka.stream.ActorAttributes.supervisionStrategy(akka.stream.Supervision.resumingDecider)
            )
            .throttle(30, 1 second)
        } { source =>
          Ok.chunked(source)
            .withHeaders(
              noProxyBufferHeader,
              CONTENT_DISPOSITION -> s"attachment; filename=${username}-${if (isMe) "all" else "public"}-studies.pgn"
            )
            .as(pgnContentType)
        }
        .fuccess
    }

  private def requestPgnFlags(req: RequestHeader) =
    lila.study.PgnDump.WithFlags(
      comments = getBoolOpt("comments", req) | true,
      variations = getBoolOpt("variations", req) | true,
      clocks = getBoolOpt("clocks", req) | true
    )

  def chapterGif(id: String, chapterId: String) =
    Open { implicit ctx =>
      env.study.api.byIdWithChapter(id, chapterId) flatMap {
        _.fold(notFound) { case WithChapter(study, chapter) =>
          CanViewResult(study) {
            env.study.gifExport.ofChapter(chapter) map { stream =>
              Ok.chunked(stream)
                .withHeaders(
                  noProxyBufferHeader,
                  CONTENT_DISPOSITION -> s"attachment; filename=${env.study.pgnDump.filename(study, chapter)}.gif"
                ) as "image/gif"
            }
          }
        }
      }
    }

  def multiBoard(id: String, page: Int) =
    Open { implicit ctx =>
      OptionFuResult(env.study.api byId id) { study =>
        CanViewResult(study) {
          env.study.multiBoard.json(study.id, page, getBool("playing")) map JsonOk
        }
      }
    }

  def topicAutocomplete =
    Action.async { req =>
      get("term", req).filter(_.nonEmpty) match {
        case None => BadRequest("No search term provided").fuccess
        case Some(term) =>
          import lila.study.JsonView._
          env.study.topicApi.findLike(term, get("user", req)) map { JsonOk(_) }
      }
    }

  def topics =
    Open { implicit ctx =>
      env.study.topicApi.popular(50) zip
        ctx.me.??(u => env.study.topicApi.userTopics(u.id) dmap some) map { case (popular, mine) =>
          val form = mine map lila.study.StudyForm.topicsForm
          Ok(html.study.topic.index(popular, mine, form))
        }
    }

  def setTopics =
    AuthBody { implicit ctx => me =>
      implicit val req = ctx.body
      lila.study.StudyForm.topicsForm
        .bindFromRequest()
        .fold(
          _ => Redirect(routes.Study.topics).fuccess,
          topics =>
            env.study.topicApi.userTopics(me, topics) inject
              Redirect(routes.Study.topics)
        )
    }

  private[controllers] def CanViewResult(
      study: StudyModel
  )(f: => Fu[Result])(implicit ctx: lila.api.Context) =
    if (canView(study)) f
    else
      negotiate(
        html = fuccess(Unauthorized(html.site.message.privateStudy(study))),
        api = _ => fuccess(Unauthorized(jsonError("This study is now private")))
      )

  private def canView(study: StudyModel)(implicit ctx: lila.api.Context) =
    !study.isPrivate || ctx.userId.exists(study.members.contains)

  implicit private def makeStudyId(id: String): StudyModel.Id = StudyModel.Id(id)
  implicit private def makeChapterId(id: String): Chapter.Id  = Chapter.Id(id)

  private[controllers] def streamsOf(
      study: StudyModel
  )(implicit ctx: Context): Fu[List[lila.streamer.Stream]] =
    env.streamer.liveStreamApi.all.flatMap {
      _.streams
        .filter { s =>
          study.members.members.exists(m => s is m._2.id)
        }
        .map { stream =>
          (fuccess(ctx.me ?? stream.streamer.is) >>|
            env.study.isConnected(study.id, stream.streamer.userId)) map { _ option stream }
        }
        .sequenceFu
        .map(_.flatten)
    }
}
