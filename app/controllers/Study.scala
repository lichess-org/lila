package controllers

import play.api.libs.json.*
import play.api.mvc.*
import scalalib.Json.given
import scalalib.paginator.Paginator

import lila.analyse.Analysis
import lila.app.{ *, given }
import lila.common.{ Bus, HTTPRequest }
import lila.core.id.RelayRoundId
import lila.core.misc.lpv.LpvEmbed
import lila.core.net.IpAddress
import lila.core.socket.Sri
import lila.core.study.Order
import lila.study.JsonView.JsData
import lila.study.PgnDump.WithFlags
import lila.study.Study.WithChapter
import lila.study.{ BecomeStudyAdmin, Who }
import lila.study.{ Chapter, Orders, Settings, Study as StudyModel, StudyForm }
import lila.tree.Node.partitionTreeJsonWriter
import com.fasterxml.jackson.core.JsonParseException

final class Study(
    env: Env,
    editorC: => Editor,
    userAnalysisC: => UserAnalysis,
    apiC: => Api
) extends LilaController(env):

  import env.user.flairApi.given

  def search(text: String, page: Int) = OpenOrScopedBody(parse.anyContent)(_.Study.Read, _.Web.Mobile):
    Reasonable(page):
      text.trim.some.filter(_.nonEmpty).filter(_.sizeIs > 2).filter(_.sizeIs < 200) match
        case None =>
          for
            pag <- env.study.pager.all(Orders.default, page)
            _   <- preloadMembers(pag)
            res <- negotiate(
              Ok.page(views.study.list.all(pag, Orders.default)),
              apiStudies(pag)
            )
          yield res
        case Some(clean) =>
          env
            .studySearch(clean.take(100), page)
            .flatMap: pag =>
              negotiate(
                Ok.page(views.study.list.search(pag, text)),
                apiStudies(pag)
              )

  def homeLang = LangPage(routes.Study.allDefault())(allResults(Order.hot, 1))

  def allDefault(page: Int) = all(Order.hot, page)

  def all(order: Order, page: Int) = OpenOrScoped(_.Study.Read, _.Web.Mobile):
    allResults(order, page)

  private def allResults(order: Order, page: Int)(using ctx: Context) =
    Reasonable(page):
      order match
        case order if !Orders.withoutSelector.contains(order) =>
          Redirect(routes.Study.allDefault(page))
        case order =>
          for
            pag <- env.study.pager.all(order, page)
            _   <- preloadMembers(pag)
            res <- negotiate(
              Ok.page(views.study.list.all(pag, order)),
              apiStudies(pag)
            )
          yield res

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

  def mine(order: Order, page: Int) = AuthOrScoped(_.Web.Mobile) { ctx ?=> me ?=>
    for
      pag <- env.study.pager.mine(order, page)
      _   <- preloadMembers(pag)
      res <- negotiate(
        env.study.topicApi.userTopics(me).flatMap { topics =>
          Ok.page(views.study.list.mine(pag, order, topics))
        },
        apiStudies(pag)
      )
    yield res
  }

  def minePublic(order: Order, page: Int) = AuthOrScoped(_.Web.Mobile) { ctx ?=> me ?=>
    for
      pag <- env.study.pager.minePublic(order, page)
      _   <- preloadMembers(pag)
      res <- negotiate(
        Ok.page(views.study.list.minePublic(pag, order)),
        apiStudies(pag)
      )
    yield res
  }

  def minePrivate(order: Order, page: Int) = AuthOrScoped(_.Web.Mobile) { ctx ?=> me ?=>
    for
      pag <- env.study.pager.minePrivate(order, page)
      _   <- preloadMembers(pag)
      res <- negotiate(
        Ok.page(views.study.list.minePrivate(pag, order)),
        apiStudies(pag)
      )
    yield res

  }

  def mineMember(order: Order, page: Int) = AuthOrScoped(_.Web.Mobile) { ctx ?=> me ?=>
    for
      pag <- env.study.pager.mineMember(order, page)
      _   <- preloadMembers(pag)
      res <- negotiate(
        Ok.async:
          env.study.topicApi
            .userTopics(me)
            .map:
              views.study.list.mineMember(pag, order, _)
        ,
        apiStudies(pag)
      )
    yield res
  }

  def mineLikes(order: Order, page: Int) = AuthOrScoped(_.Web.Mobile) { ctx ?=> me ?=>
    for
      pag <- env.study.pager.mineLikes(order, page)
      _   <- preloadMembers(pag)
      res <- negotiate(
        Ok.page(views.study.list.mineLikes(pag, order)),
        apiStudies(pag)
      )
    yield res
  }

  def byTopic(name: String, order: Order, page: Int) = Open:
    Found(lila.study.StudyTopic.fromStr(name)): topic =>
      for
        pag    <- env.study.pager.byTopic(topic, order, page)
        _      <- preloadMembers(pag)
        topics <- ctx.userId.soFu(env.study.topicApi.userTopics)
        res <- negotiate(
          Ok.async:
            ctx.userId
              .soFu(env.study.topicApi.userTopics)
              .map:
                views.study.list.topic.show(topic, pag, order, _)
          ,
          apiStudies(pag)
        )
      yield res

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
        .byIdWithTour(id.into(RelayRoundId))
        .flatMap:
          _.fold(f): rt =>
            Redirect(chapterId.fold(rt.path)(rt.path))
    else f

  private def showQuery(query: Fu[Option[WithChapter]])(using ctx: Context): Fu[Result] =
    Found(query): oldSc =>
      CanView(oldSc.study) {
        negotiate(
          html =
            val noCrawler = HTTPRequest.isCrawler(ctx.req).no
            for
              (sc, data) <- getJsonData(oldSc, withChapters = true)
              chat       <- noCrawler.so(chatOf(sc.study))
              sVersion   <- noCrawler.so(env.study.version(sc.study.id))
              streamers  <- noCrawler.so(streamerCache.get(sc.study.id))
              page       <- renderPage(views.study.show(sc.study, data, chat, sVersion, streamers))
            yield Ok(page)
              .withCanonical(routes.Study.chapter(sc.study.id, sc.chapter.id))
              .enforceCrossSiteIsolation
          ,
          json = for
            (sc, data) <- getJsonData(
              oldSc,
              withChapters = getBool("chapters") || HTTPRequest.isLichobile(ctx.req)
            )
            loadChat = !HTTPRequest.isXhr(ctx.req)
            chatOpt <- loadChat.so(chatOf(sc.study))
            jsChat <- chatOpt.soFu: c =>
              lila.chat.JsonView.mobile(c.chat, writeable = ctx.userId.so(sc.study.canChat))
          yield Ok:
            Json.obj(
              "study"    -> data.study.add("chat" -> jsChat),
              "analysis" -> data.analysis
            )
        )
      }(privateUnauthorizedFu(oldSc.study), privateForbiddenFu(oldSc.study))
    .dmap(_.noCache)

  private[controllers] def getJsonData(sc: WithChapter, withChapters: Boolean)(using
      ctx: Context
  ): Fu[(WithChapter, JsData)] =
    for
      (study, chapter) <- env.study.api.maybeResetAndGetChapter(sc.study, sc.chapter)
      previews         <- withChapters.soFu(env.study.preview.jsonList(study.id))
      _                <- env.user.lightUserApi.preloadMany(study.members.ids.toList)
      fedNames         <- env.study.preview.federations.get(sc.study.id)
      pov = userAnalysisC.makePov(chapter.root.fen.some, chapter.setup.variant)
      analysis <- chapter.serverEval
        .exists(_.done)
        .so(env.analyse.analyser.byId(Analysis.Id(study.id, chapter.id)))
      division = analysis.isDefined.option(env.study.serverEvalMerger.divisionOf(chapter))
      baseData <- env.analyse.externalEngine.withExternalEngines(
        env.round.jsonView.userAnalysisJson(
          pov,
          ctx.pref,
          chapter.root.fen.some,
          chapter.setup.orientation,
          owner = false,
          division = division
        )
      )
      withMembers = !study.isRelay || isGrantedOpt(_.StudyAdmin) || ctx.me.exists(study.isMember)
      studyJson <- env.study.jsonView.full(study, chapter, previews, fedNames.some, withMembers = withMembers)
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

  def show(id: StudyId) = OpenOrScoped(_.Study.Read, _.Web.Mobile):
    orRelayRedirect(id):
      showQuery(env.study.api.byIdWithChapter(id))

  def chapter(id: StudyId, chapterId: StudyChapterId) = OpenOrScoped(_.Study.Read, _.Web.Mobile):
    orRelayRedirect(id, chapterId.some):
      env.study.api
        .byIdWithChapter(id, chapterId)
        .flatMap:
          case None =>
            env.study.studyRepo
              .exists(id)
              .flatMap:
                if _ then negotiate(Redirect(routes.Study.show(id)), notFoundJson())
                else showQuery(fuccess(none))
          case sc => showQuery(fuccess(sc))

  def chapterConfig(id: StudyId, chapterId: StudyChapterId) = Open:
    Found(env.study.chapterRepo.byIdAndStudy(chapterId, id)): chapter =>
      Ok(env.study.jsonView.chapterConfig(chapter))

  private[controllers] def chatOf(study: lila.study.Study)(using ctx: Context) = {
    ctx.kid.no && ctx.noBot // no public chats for kids and bots
  }.soFu:
    env.chat.api.userChat
      .findMine(study.id.into(ChatId))
      .mon(_.chat.fetch("study"))

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
      for
        round <- env.relay.api.deleteRound(id.into(RelayRoundId))
        _     <- env.study.api.delete(study)
      yield round match
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
      .flatMapz:
        env.chat.api.userChat.clear(id.into(ChatId))
      .inject(Redirect(routes.Study.show(id)))
  }

  private def doImportPgn(id: StudyId, data: StudyForm.importPgn.Data, sri: Sri)(
      f: List[Chapter] => Result
  )(using ctx: Context, me: Me): Future[Result] =
    val chapterDatas = data.toChapterDatas
    limit.studyPgnImport(me, rateLimited, cost = chapterDatas.size):
      env.study.api
        .importPgns(id, chapterDatas, sticky = data.sticky, ctx.pref.showRatings)(Who(me, sri))
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
          import lila.study.ChapterPreview.json.given
          val previews = chapters.map(env.study.preview.fromChapter(_)(using Map.empty))
          JsonOk(Json.obj("chapters" -> previews))
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
      def notFound = NotFound.snip(views.study.embed.notFound)
      env.study.api
        .byId(studyId)
        .flatMap:
          _.fold(notFound.toFuccess): study =>
            val finalChapterId = if chapterId.value == "autochap" then study.position.chapterId else chapterId
            env.api.textLpvExpand
              .getChapterPgn(finalChapterId)
              .map:
                case Some(LpvEmbed.PublicPgn(pgn)) => Ok.snip(views.study.embed(study, finalChapterId, pgn))
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
    pgnWithFlags(id, identity)

  def apiPgn(id: StudyId) = AnonOrScoped(_.Study.Read, _.Web.Mobile): ctx ?=>
    pgnWithFlags(id, identity)

  def pgnWithFlags(id: StudyId, flags: Update[WithFlags])(using Context) =
    Found(env.study.api.byId(id)): study =>
      HeadLastModifiedAt(study.updatedAt):
        val limiter = if study.isRelay then limit.relayPgn else limit.studyPgn
        limiter[Fu[Result]](req.ipAddress, rateLimited, msg = id.value):
          CanView(study, study.settings.shareable.some)(doPgn(study, flags))(
            privateUnauthorizedFu(study),
            privateForbiddenFu(study)
          )

  private def doPgn(study: StudyModel, flags: Update[WithFlags])(using RequestHeader, Option[Me]) =
    Ok.chunked(env.study.pgnDump.chaptersOf(study, flags(requestPgnFlags)).throttle(20, 1.second))
      .pipe(asAttachmentStream(s"${env.study.pgnDump.filename(study)}.pgn"))
      .as(pgnContentType)
      .withDateHeaders(lastModified(study.updatedAt))

  def chapterPgn(id: StudyId, chapterId: StudyChapterId) = Open:
    doChapterPgn(id, chapterId, notFound, privateUnauthorizedFu, privateForbiddenFu)

  def apiChapterPgn(id: StudyId, chapterId: StudyChapterId) =
    AnonOrScoped(_.Study.Read, _.Web.Mobile): ctx ?=>
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
    env.study.api
      .byIdWithChapter(id, chapterId)
      .flatMap:
        _.fold(studyNotFound) { case WithChapter(study, chapter) =>
          CanView(study) {
            env.study.pgnDump.ofChapter(study, requestPgnFlags)(chapter).map { pgn =>
              Ok(pgn.toString)
                .pipe(asAttachment(s"${env.study.pgnDump.filename(study, chapter)}.pgn"))
                .as(pgnContentType)
            }
          }(studyUnauthorized(study), studyForbidden(study))
        }

  def exportPgn(username: UserStr) = OpenOrScoped(_.Study.Read, _.Web.Mobile): ctx ?=>
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

  def apiListByOwner(username: UserStr) = OpenOrScoped(_.Study.Read, _.Web.Mobile): ctx ?=>
    val isMe = ctx.is(username)
    apiC.jsonDownload:
      env.study.studyRepo
        .sourceByOwner(username.id, isMe)
        .throttle(if isMe then 50 else 20, 1.second)
        .map(lila.study.JsonView.metadata)

  private def requestPgnFlags(using RequestHeader) =
    WithFlags(
      comments = getBoolOpt("comments") | true,
      variations = getBoolOpt("variations") | true,
      clocks = getBoolOpt("clocks") | true,
      source = getBool("source"),
      orientation = getBool("orientation"),
      site = none
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
    env.study.topicApi
      .popular(50)
      .zip(ctx.userId.soFu(env.study.topicApi.userTopics))
      .flatMap: (popular, mine) =>
        val form = mine.map(StudyForm.topicsForm)
        Ok.page(views.study.list.topic.index(popular, mine, form))

  def setTopics = AuthBody { ctx ?=> me ?=>
    bindForm(StudyForm.topicsForm)(
      _ => Redirect(routes.Study.topics),
      topics =>
        try env.study.topicApi.userTopics(me, topics).inject(Redirect(routes.Study.topics))
        catch case e: JsonParseException => BadRequest(e.getMessage)
    )
  }

  def staffPicks = Open:
    pageHit
    FoundPage(env.cms.renderKey("studies-staff-picks")):
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
      if userSelection.forall(Settings.UserSelection.allows(_, study, me.map(_.userId))) then f
      else forbidden
    me match
      case _ if !study.isPrivate                        => withUserSelection
      case None                                         => unauthorized
      case Some(me) if study.members.contains(me.value) => withUserSelection
      case _                                            => forbidden

  private val streamerCache =
    env.memo.cacheApi[StudyId, List[UserId]](64, "study.streamers"):
      _.expireAfterWrite(10.seconds).buildAsyncFuture: studyId =>
        env.study.findConnectedUsersIn(studyId)(env.streamer.liveStreamApi.streamerUserIds)

  def glyphs(lang: String) = Anon:
    Found(play.api.i18n.Lang.get(lang)): lang =>
      JsonOk:
        lila.study.JsonView.glyphs(using env.translator.to(lang))
      .withHeaders(CACHE_CONTROL -> "max-age=3600")
