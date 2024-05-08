package controllers

import play.api.libs.json.*
import play.api.mvc.*
import scalalib.Json.given

import scala.util.chaining.*

import lila.analyse.Analysis
import lila.app.{ *, given }
import scalalib.paginator.Paginator
import lila.common.{ Bus, HTTPRequest }
import lila.core.socket.Sri
import lila.study.JsonView.JsData
import lila.study.Study.WithChapter
import lila.study.actorApi.{ BecomeStudyAdmin, Who }
import lila.study.{ Chapter, Settings, Orders, Study as StudyModel, StudyForm }
import lila.tree.Node.partitionTreeJsonWriter
import lila.core.misc.lpv.LpvEmbed
import lila.core.net.IpAddress
import lila.core.id.RelayRoundId
import lila.core.study.Order

final class Study(
    env: Env,
    editorC: => Editor,
    userAnalysisC: => UserAnalysis,
    apiC: => Api
) extends LilaController(env):

  import env.user.flairApi.given

  def search(text: String, page: Int) = OpenBody:
    Reasonable(page):
      if text.trim.isEmpty then
        env.study.pager
          .all(Orders.default, page)
          .flatMap: pag =>
            preloadMembers(pag) >> negotiate(
              Ok.page(views.study.list.all(pag, Orders.default)),
              apiStudies(pag)
            )
      else
        env
          .studySearch(ctx.me)(text, page)
          .flatMap: pag =>
            negotiate(
              Ok.page(views.study.list.search(pag, text)),
              apiStudies(pag)
            )

  def homeLang = LangPage(routes.Study.allDefault())(allResults(Order.hot, 1))

  def allDefault(page: Int) = all(Order.hot, page)

  def all(order: Order, page: Int) = Open:
    allResults(order, page)

  private def allResults(order: Order, page: Int)(using ctx: Context) =
    Reasonable(page):
      order match
        case order if !Orders.withoutSelector.contains(order) =>
          Redirect(routes.Study.allDefault(page))
        case order =>
          env.study.pager
            .all(order, page)
            .flatMap: pag =>
              preloadMembers(pag) >> negotiate(
                Ok.page(views.study.list.all(pag, order)),
                apiStudies(pag)
              )

  def byOwnerDefault(username: UserStr, page: Int) = byOwner(username, Orders.default, page)

  def byOwner(username: UserStr, order: Order, page: Int) = Open:
    Found(meOrFetch(username)): owner =>
      env.study.pager
        .byOwner(owner, order, page)
        .flatMap: pag =>
          preloadMembers(pag) >> negotiate(
            Ok.page(views.study.list.byOwner(pag, order, owner)),
            apiStudies(pag)
          )

  def mine(order: Order, page: Int) = Auth { ctx ?=> me ?=>
    env.study.pager
      .mine(order, page)
      .flatMap: pag =>
        preloadMembers(pag) >> negotiate(
          env.study.topicApi.userTopics(me).flatMap { topics =>
            Ok.page(views.study.list.mine(pag, order, topics))
          },
          apiStudies(pag)
        )
  }

  def minePublic(order: Order, page: Int) = Auth { ctx ?=> me ?=>
    env.study.pager
      .minePublic(order, page)
      .flatMap: pag =>
        preloadMembers(pag) >> negotiate(
          Ok.page(views.study.list.minePublic(pag, order)),
          apiStudies(pag)
        )
  }

  def minePrivate(order: Order, page: Int) = Auth { ctx ?=> me ?=>
    env.study.pager
      .minePrivate(order, page)
      .flatMap: pag =>
        preloadMembers(pag) >> negotiate(
          Ok.page(views.study.list.minePrivate(pag, order)),
          apiStudies(pag)
        )
  }

  def mineMember(order: Order, page: Int) = Auth { ctx ?=> me ?=>
    env.study.pager
      .mineMember(order, page)
      .flatMap: pag =>
        preloadMembers(pag) >> negotiate(
          Ok.async:
            env.study.topicApi
              .userTopics(me)
              .map:
                views.study.list.mineMember(pag, order, _)
          ,
          apiStudies(pag)
        )
  }

  def mineLikes(order: Order, page: Int) = Auth { ctx ?=> me ?=>
    env.study.pager
      .mineLikes(order, page)
      .flatMap: pag =>
        preloadMembers(pag) >> negotiate(
          Ok.page(views.study.list.mineLikes(pag, order)),
          apiStudies(pag)
        )
  }

  def byTopic(name: String, order: Order, page: Int) = Open:
    Found(lila.study.StudyTopic.fromStr(name)): topic =>
      env.study.pager
        .byTopic(topic, order, page)
        .zip(ctx.userId.soFu(env.study.topicApi.userTopics))
        .flatMap: (pag, topics) =>
          preloadMembers(pag) >> Ok.page(views.study.list.topic.show(topic, pag, order, topics))

  private def preloadMembers(pag: Paginator[StudyModel.WithChaptersAndLiked]) =
    env.user.lightUserApi.preloadMany(
      pag.currentPageResults.view
        .flatMap(_.study.members.members.values.take(StudyModel.previewNbMembers))
        .map(_.id)
        .toSeq
    )

  private def apiStudies(pager: Paginator[StudyModel.WithChaptersAndLiked]) =
    given Writes[StudyModel.WithChaptersAndLiked] = Writes(env.study.jsonView.pagerData)
    Ok(Json.obj("paginator" -> pager))

  private def orRelayRedirect(id: StudyId, chapterId: Option[StudyChapterId] = None)(
      f: => Fu[Result]
  )(using ctx: Context): Fu[Result] =
    if HTTPRequest.isRedirectable(ctx.req)
    then
      env.relay.api
        .getOngoing(id.into(RelayRoundId))
        .flatMap:
          _.fold(f): rt =>
            Redirect(chapterId.fold(rt.path)(rt.path))
    else f

  private def showQuery(query: Fu[Option[WithChapter]])(using ctx: Context): Fu[Result] =
    Found(query): oldSc =>
      CanView(oldSc.study) {
        for
          (sc, data) <- getJsonData(oldSc)
          res <- negotiate(
            html =
              val noCrawler = HTTPRequest.isCrawler(ctx.req).no
              for
                chat      <- noCrawler.so(chatOf(sc.study))
                sVersion  <- noCrawler.so(env.study.version(sc.study.id))
                streamers <- noCrawler.so(streamerCache.get(sc.study.id))
                page      <- renderPage(views.study.show(sc.study, data, chat, sVersion, streamers))
              yield Ok(page)
                .withCanonical(routes.Study.chapter(sc.study.id, sc.chapter.id))
                .enforceCrossSiteIsolation
            ,
            json = for
              chatOpt <- chatOf(sc.study)
              jsChat <- chatOpt.soFu: c =>
                lila.chat.JsonView.mobile(c.chat, writeable = ctx.userId.so(sc.study.canChat))
            yield Ok:
              Json.obj(
                "study"    -> data.study.add("chat" -> jsChat),
                "analysis" -> data.analysis
              )
          )
        yield res
      }(privateUnauthorizedFu(oldSc.study), privateForbiddenFu(oldSc.study))
    .dmap(_.noCache)

  private[controllers] def getJsonData(sc: WithChapter)(using
      ctx: Context
  ): Fu[(WithChapter, JsData)] =
    for
      (study, chapter, previews) <-
        env.study.api.maybeResetAndGetChapterPreviews(sc.study, sc.chapter)
      _        <- env.user.lightUserApi.preloadMany(study.members.ids.toList)
      fedNames <- env.study.preview.federations.get(sc.study.id)
      pov = userAnalysisC.makePov(chapter.root.fen.some, chapter.setup.variant)
      analysis <- chapter.serverEval
        .exists(_.done)
        .so(env.analyse.analyser.byId(Analysis.Id(study.id, chapter.id)))
      division = analysis.isDefined.option(env.study.serverEvalMerger.divisionOf(chapter))
      baseData <- env.api.roundApi.withExternalEngines(
        ctx.me,
        env.round.jsonView.userAnalysisJson(
          pov,
          ctx.pref,
          chapter.root.fen.some,
          chapter.setup.orientation,
          owner = false,
          division = division
        )
      )
      studyJson <- env.study.jsonView(study, previews, chapter, fedNames.some, withMembers = !study.isRelay)
    yield WithChapter(study, chapter) -> JsData(
      study = studyJson,
      analysis = baseData
        .add(
          "treeParts" -> partitionTreeJsonWriter.writes {
            lila.study.TreeBuilder(chapter.root, chapter.setup.variant)
          }.some
        )
        .add("analysis" -> analysis.map { env.analyse.jsonView.bothPlayers(chapter.root.ply, _) })
    )

  def show(id: StudyId) = Open:
    orRelayRedirect(id):
      showQuery(env.study.api.byIdWithChapter(id))

  def chapter(id: StudyId, chapterId: StudyChapterId) =
    Open:
      orRelayRedirect(id, chapterId.some):
        env.study.api
          .byIdWithChapter(id, chapterId)
          .flatMap:
            case None =>
              env.study.studyRepo
                .exists(id)
                .flatMap:
                  if _ then Redirect(routes.Study.show(id))
                  else showQuery(fuccess(none))
            case sc => showQuery(fuccess(sc))

  def chapterConfig(id: StudyId, chapterId: StudyChapterId) = Open:
    Found(env.study.chapterRepo.byIdAndStudy(chapterId, id)): chapter =>
      Ok(env.study.jsonView.chapterConfig(chapter))

  private[controllers] def chatOf(study: lila.study.Study)(using ctx: Context) = {
    ctx.kid.no && ctx.noBot &&               // no public chats for kids and bots
    ctx.me.forall(env.chat.panic.allowed(_)) // anon can see public chats
  }.soFu(
    env.chat.api.userChat
      .findMine(study.id.into(ChatId))
      .mon(_.chat.fetch("study"))
  )

  def createAs = AuthBody { ctx ?=> me ?=>
    bindForm(StudyForm.importGame.form)(
      _ => Redirect(routes.Study.byOwnerDefault(me.username)),
      data =>
        for
          owner   <- env.study.api.recentByOwnerWithChapterCount(me, 50)
          contrib <- env.study.api.recentByContributorWithChapterCount(me, 50)
          res <-
            if owner.isEmpty && contrib.isEmpty then createStudy(data)
            else
              val back = HTTPRequest
                .referer(ctx.req)
                .orElse(
                  data.fen.map(fen => editorC.editorUrl(fen, data.variant | chess.variant.Variant.default))
                )
              Ok.page(views.study.create(data, owner, contrib, back))
        yield res
    )
  }

  def create = AuthBody { ctx ?=> me ?=>
    bindForm(StudyForm.importGame.form)(
      _ => Redirect(routes.Study.byOwnerDefault(me.username)),
      createStudy
    )
  }

  private def createStudy(data: StudyForm.importGame.Data)(using ctx: Context, me: Me) =
    Found(env.study.api.importGame(lila.study.StudyMaker.ImportGame(data), me, ctx.pref.showRatings)): sc =>
      Redirect(routes.Study.chapter(sc.study.id, sc.chapter.id))

  def delete(id: StudyId) = Auth { _ ?=> me ?=>
    Found(env.study.api.byIdAndOwnerOrAdmin(id, me)): study =>
      env.study.api.delete(study) >> env.relay.api
        .deleteRound(id.into(RelayRoundId))
        .map:
          case None       => Redirect(routes.Study.mine(Order.hot))
          case Some(tour) => Redirect(routes.RelayTour.show(tour.slug, tour.id))
  }

  def apiChapterDelete(id: StudyId, chapterId: StudyChapterId) = ScopedBody(_.Study.Write) { _ ?=> me ?=>
    Found(env.study.api.byIdAndOwnerOrAdmin(id, me)): study =>
      env.study.api.deleteChapter(id, chapterId)(Who(me.userId, Sri("api"))).inject(NoContent)
  }

  def clearChat(id: StudyId) = Auth { _ ?=> me ?=>
    env.study.api
      .isOwnerOrAdmin(id, me)
      .flatMapz {
        env.chat.api.userChat.clear(id.into(ChatId))
      }
      .inject(Redirect(routes.Study.show(id)))
  }

  private def doImportPgn(id: StudyId, data: StudyForm.importPgn.Data, sri: Sri)(
      f: List[Chapter] => Result
  )(using ctx: Context, me: Me): Future[Result] =
    val chapterDatas = data.toChapterDatas
    limit.studyPgnImport(me, rateLimited, cost = chapterDatas.size):
      env.study.api
        .importPgns(
          id,
          chapterDatas,
          sticky = data.sticky,
          ctx.pref.showRatings
        )(Who(me, sri))
        .map(f)

  def importPgn(id: StudyId) = AuthBody { ctx ?=> me ?=>
    get("sri").so: sri =>
      bindForm(StudyForm.importPgn.form)(
        doubleJsonFormError,
        data => doImportPgn(id, data, Sri(sri))(_ => NoContent)
      )
  }

  def apiImportPgn(id: StudyId) = ScopedBody(_.Study.Write) { ctx ?=> me ?=>
    bindForm(StudyForm.importPgn.form)(
      jsonFormError,
      data =>
        doImportPgn(id, data, Sri("api")): chapters =>
          import lila.study.ChapterPreview.json.write
          JsonOk(Json.obj("chapters" -> write(chapters.map(_.preview))(using Map.empty)))
    )
  }

  def admin(id: StudyId) = Secure(_.StudyAdmin) { ctx ?=> me ?=>
    Bus.publish(BecomeStudyAdmin(id, me), "adminStudy")
    env.study.api
      .becomeAdmin(id, me)
      .inject:
        if HTTPRequest.isXhr(ctx.req) then NoContent else Redirect(routes.Study.show(id))
  }

  def embed(studyId: StudyId, chapterId: StudyChapterId) = Anon:
    InEmbedContext:
      val studyFu =
        if chapterId.value == "autochap"
        then env.study.api.byIdWithChapter(studyId)
        else env.study.api.byIdWithChapterOrFallback(studyId, chapterId)
      def notFound = NotFound.snip(views.study.embed.notFound)
      studyFu
        .flatMap:
          _.fold(notFound.toFuccess): sc =>
            env.api.textLpvExpand
              .getChapterPgn(sc.chapter.id)
              .map:
                case Some(LpvEmbed.PublicPgn(pgn)) => Ok.snip(views.study.embed(sc.study, sc.chapter, pgn))
                case _                             => notFound

  def cloneStudy(id: StudyId) = Auth { ctx ?=> _ ?=>
    Found(env.study.api.byId(id)): study =>
      CanView(study, study.settings.cloneable.some) {
        Ok.page(views.study.clone(study))
      }(privateUnauthorizedFu(study), privateForbiddenFu(study))
  }

  def cloneApply(id: StudyId) = Auth { ctx ?=> me ?=>
    val cost = if isGranted(_.Coach) || me.hasTitle then 1 else 3
    limit.studyClone(me.userId -> ctx.ip, rateLimited, cost):
      Found(env.study.api.byId(id)) { prev =>
        CanView(prev, prev.settings.cloneable.some) {
          env.study.api
            .cloneWithChat(me, prev)
            .map: study =>
              Redirect(routes.Study.show((study | prev).id))
        }(privateUnauthorizedFu(prev), privateForbiddenFu(prev))
      }
  }

  def pgn(id: StudyId) = Open:
    Found(env.study.api.byId(id)): study =>
      HeadLastModifiedAt(study.updatedAt):
        limit.studyPgn(ctx.ip, rateLimited, msg = id.value):
          CanView(study, study.settings.shareable.some)(doPgn(study))(
            privateUnauthorizedFu(study),
            privateForbiddenFu(study)
          )

  def apiPgn(id: StudyId) = AnonOrScoped(_.Study.Read): ctx ?=>
    env.study.api.byId(id).flatMap {
      _.fold(studyNotFoundText.toFuccess): study =>
        HeadLastModifiedAt(study.updatedAt):
          limit.studyPgn[Fu[Result]](req.ipAddress, rateLimited, msg = id.value):
            CanView(study, study.settings.shareable.some)(doPgn(study))(
              privateUnauthorizedText,
              privateForbiddenText
            )
    }

  private def doPgn(study: StudyModel)(using RequestHeader, Option[Me]) =
    Ok.chunked(env.study.pgnDump.chaptersOf(study, requestPgnFlags).throttle(16, 1.second))
      .pipe(asAttachmentStream(s"${env.study.pgnDump.filename(study)}.pgn"))
      .as(pgnContentType)
      .withDateHeaders(lastModified(study.updatedAt))

  def chapterPgn(id: StudyId, chapterId: StudyChapterId) = Open:
    doChapterPgn(id, chapterId, notFound, privateUnauthorizedFu, privateForbiddenFu)

  def apiChapterPgn(id: StudyId, chapterId: StudyChapterId) = AnonOrScoped(_.Study.Read): ctx ?=>
    doChapterPgn(
      id,
      chapterId,
      fuccess(studyNotFoundText),
      _ => fuccess(privateUnauthorizedText),
      _ => fuccess(privateForbiddenText)
    )

  private def doChapterPgn(
      id: StudyId,
      chapterId: StudyChapterId,
      studyNotFound: => Fu[Result],
      studyUnauthorized: StudyModel => Fu[Result],
      studyForbidden: StudyModel => Fu[Result]
  )(using ctx: Context) =
    env.study.api.byIdWithChapter(id, chapterId).flatMap {
      _.fold(studyNotFound) { case WithChapter(study, chapter) =>
        CanView(study) {
          env.study.pgnDump.ofChapter(study, requestPgnFlags)(chapter).map { pgn =>
            Ok(pgn.toString)
              .pipe(asAttachment(s"${env.study.pgnDump.filename(study, chapter)}.pgn"))
              .as(pgnContentType)
          }
        }(studyUnauthorized(study), studyForbidden(study))
      }
    }

  def exportPgn(username: UserStr) = OpenOrScoped(_.Study.Read): ctx ?=>
    val name =
      if username.value == "me"
      then ctx.me.fold(UserName("me"))(_.username)
      else username.into(UserName)
    val userId = name.id
    val isMe   = ctx.me.exists(_.is(userId))
    val makeStream = env.study.studyRepo
      .sourceByOwner(userId, isMe)
      .flatMapConcat(env.study.pgnDump.chaptersOf(_, requestPgnFlags))
      .throttle(16, 1.second)
      .withAttributes:
        akka.stream.ActorAttributes.supervisionStrategy(akka.stream.Supervision.resumingDecider)
    apiC.GlobalConcurrencyLimitPerIpAndUserOption(userId.some)(makeStream): source =>
      Ok.chunked(source)
        .pipe(asAttachmentStream(s"${name}-${if isMe then "all" else "public"}-studies.pgn"))
        .as(pgnContentType)

  def apiListByOwner(username: UserStr) = OpenOrScoped(_.Study.Read): ctx ?=>
    val isMe = ctx.is(username)
    apiC.jsonDownload:
      env.study.studyRepo
        .sourceByOwner(username.id, isMe)
        .throttle(if isMe then 50 else 20, 1.second)
        .map(lila.study.JsonView.metadata)

  private def requestPgnFlags(using RequestHeader) =
    lila.study.PgnDump.WithFlags(
      comments = getBoolOpt("comments") | true,
      variations = getBoolOpt("variations") | true,
      clocks = getBoolOpt("clocks") | true,
      source = getBool("source"),
      orientation = getBool("orientation")
    )

  def chapterGif(id: StudyId, chapterId: StudyChapterId, theme: Option[String], piece: Option[String]) = Open:
    Found(env.study.api.byIdWithChapter(id, chapterId)):
      case WithChapter(study, chapter) =>
        CanView(study) {
          env.study.gifExport
            .ofChapter(chapter, theme, piece)
            .map: stream =>
              Ok.chunked(stream)
                .pipe(asAttachmentStream(s"${env.study.pgnDump.filename(study, chapter)}.gif"))
                .as("image/gif")
            .recover { case lila.core.lilaism.LilaInvalid(msg) =>
              BadRequest(msg)
            }
        }(privateUnauthorizedFu(study), privateForbiddenFu(study))

  def topicAutocomplete = Anon:
    get("term").filter(_.nonEmpty) match
      case None => BadRequest("No search term provided")
      case Some(term) =>
        import lila.common.Json.given
        env.study.topicApi.findLike(term, getUserStr("user").map(_.id)).map { JsonOk(_) }

  def topics = Open:
    env.study.topicApi.popular(50).zip(ctx.userId.soFu(env.study.topicApi.userTopics)).flatMap {
      (popular, mine) =>
        val form = mine.map(StudyForm.topicsForm)
        Ok.page(views.study.list.topic.index(popular, mine, form))
    }

  def setTopics = AuthBody { ctx ?=> me ?=>
    bindForm(StudyForm.topicsForm)(
      _ => Redirect(routes.Study.topics),
      topics => env.study.topicApi.userTopics(me, topics).inject(Redirect(routes.Study.topics))
    )
  }

  def staffPicks = Open:
    pageHit
    FoundPage(env.api.cmsRenderKey("studies-staff-picks")):
      views.study.staffPicks

  def privateUnauthorizedText = Unauthorized("This study is now private")
  def privateUnauthorizedJson = Unauthorized(jsonError("This study is now private"))
  def privateUnauthorizedFu(study: StudyModel)(using Context) = negotiate(
    Unauthorized.page(views.study.privateStudy(study)),
    privateUnauthorizedJson
  )

  def privateForbiddenText = Forbidden("This study is now private")
  def privateForbiddenJson = forbiddenJson("This study is now private")
  def privateForbiddenFu(study: StudyModel)(using Context) = negotiate(
    Forbidden.page(views.study.privateStudy(study)),
    privateForbiddenJson
  )

  def studyNotFoundText = NotFound("Study or chapter not found")
  def studyNotFoundJson = NotFound(jsonError("Study or chapter not found"))

  def CanView(study: StudyModel, userSelection: Option[Settings.UserSelection] = none)(
      f: => Fu[Result]
  )(unauthorized: => Fu[Result], forbidden: => Fu[Result])(using me: Option[Me]): Fu[Result] =
    def withUserSelection =
      if userSelection.fold(true)(Settings.UserSelection.allows(_, study, me.map(_.userId))) then f
      else forbidden
    me match
      case _ if !study.isPrivate                        => withUserSelection
      case None                                         => unauthorized
      case Some(me) if study.members.contains(me.value) => withUserSelection
      case _                                            => forbidden

  private val streamerCache =
    env.memo.cacheApi[StudyId, List[UserId]](1024, "study.streamers"):
      _.expireAfterWrite(10.seconds).buildAsyncFuture: studyId =>
        env.study.findConnectedUsersIn(studyId)(env.streamer.liveStreamApi.streamerUserIds)

  def glyphs(lang: String) = Anon:
    Found(play.api.i18n.Lang.get(lang)): lang =>
      JsonOk:
        lila.study.JsonView.glyphs(using env.translator.to(lang))
      .withHeaders(CACHE_CONTROL -> "max-age=3600")
