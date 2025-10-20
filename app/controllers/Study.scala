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
import lila.core.data.ErrorMsg
import lila.study.JsonView.JsData
import lila.study.PgnDump.WithFlags
import lila.study.Study.WithChapter
import lila.study.{ BecomeStudyAdmin, Who }
import lila.study.{ Chapter, Orders, Settings, Study as StudyModel, StudyForm }
import lila.tree.Node.partitionTreeJsonWriter
import com.fasterxml.jackson.core.JsonParseException
import lila.ui.Page

final class Study(
    env: Env,
    editorC: => Editor,
    userAnalysisC: => UserAnalysis,
    apiC: => Api
) extends LilaController(env):

  def search(text: String, page: Int, order: Option[Order]) =
    OpenOrScopedBody(parse.anyContent)(_.Study.Read, _.Web.Mobile):
      Reasonable(page):
        WithProxy: proxy ?=>
          val maxLen =
            if proxy.isFloodish then 50
            else if HTTPRequest.isCrawler(req).yes then 80
            else if ctx.isAnon then 100
            else 200
          text.trim.some.filter(_.nonEmpty).filter(_.sizeIs > 2).filter(_.sizeIs < maxLen) match
            case None =>
              for
                pag <- env.study.pager.all(Orders.default, page)
                _ <- preloadMembers(pag)
                res <- negotiate(
                  Ok.page(views.study.list.all(pag, Orders.default)),
                  apiStudies(pag)
                )
              yield res
            case Some(clean) =>
              limit.enumeration.search(rateLimited):
                env
                  .studySearch(clean.take(100), order | Order.relevant, page)
                  .flatMap: pag =>
                    negotiate(
                      Ok.page(views.study.list.search(pag, order | Order.relevant, text)),
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
            _ <- preloadMembers(pag)
            res <- negotiate(
              Ok.page(views.study.list.all(pag, order)),
              apiStudies(pag)
            )
          yield res

  def byOwnerDefault(username: UserStr, page: Int) = byOwner(username, Orders.default, page)

  def byOwner(username: UserStr, order: Order, page: Int) = Open:
    Found(meOrFetch(username)): owner =>
      for
        pag <- env.study.pager.byOwner(owner, order, page)
        _ <- preloadMembers(pag)
        res <- negotiate(Ok.page(views.study.list.byOwner(pag, order, owner)), apiStudies(pag))
      yield res

  def mine = MyStudyPager(
    env.study.pager.mine,
    (pag, order) => env.study.topicApi.userTopics(summon[Me]).map(views.study.list.mine(pag, order, _))
  )

  def minePublic = MyStudyPager(env.study.pager.minePublic, views.study.list.minePublic)

  def minePrivate = MyStudyPager(env.study.pager.minePrivate, views.study.list.minePrivate)

  def mineMember = MyStudyPager(
    env.study.pager.mineMember,
    (pag, order) => env.study.topicApi.userTopics(summon[Me]).map(views.study.list.mineMember(pag, order, _))
  )

  def mineLikes = MyStudyPager(env.study.pager.mineLikes, views.study.list.mineLikes)

  private type StudyPager = Paginator[StudyModel.WithChaptersAndLiked]

  private def MyStudyPager(
      makePager: (Order, Int) => Me ?=> Fu[StudyPager],
      render: (StudyPager, Order) => Context ?=> Me ?=> Fu[Page]
  ) = (order: Order, page: Int) =>
    AuthOrScoped(_.Web.Mobile) { ctx ?=> me ?=>
      for
        pager <- makePager(order, page)
        _ <- preloadMembers(pager)
        res <- negotiate(Ok.async(render(pager, order)), apiStudies(pager))
      yield res
    }

  def byTopic(name: String, order: Order, page: Int) = Open:
    Found(lila.study.StudyTopic.fromStr(name)): topic =>
      for
        pag <- env.study.pager.byTopic(topic, order, page)
        _ <- preloadMembers(pag)
        res <- negotiate(
          Ok.async:
            ctx.userId
              .traverse(env.study.topicApi.userTopics)
              .map(views.study.list.topic.show(topic, pag, order, _))
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

  private def showQuery(query: Option[WithChapter])(using ctx: Context): Fu[Result] =
    Found(query): oldSc =>
      CanView(oldSc.study) {
        if !oldSc.study.notable && HTTPRequest.isCrawler(req).yes
        then notFound
        else
          negotiate(
            html =
              val noCrawler = HTTPRequest.isCrawler(ctx.req).no
              for
                (sc, data) <- getJsonData(oldSc, withChapters = true)
                chat <- noCrawler.so(chatOf(sc.study))
                sVersion <- noCrawler.so(env.study.version(sc.study.id))
                streamers <- noCrawler.so(streamerCache.get(sc.study.id))
                page <- renderPage(views.study.show(sc.study, data, chat, sVersion, streamers))
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
              jsChat <- chatOpt.traverse: c =>
                env.chat.json.mobile(c.chat, writeable = ctx.userId.so(sc.study.canChat))
            yield Ok:
              Json.obj(
                "study" -> data.study.add("chat" -> jsChat),
                "analysis" -> data.analysis
              )
          )
      }(privateUnauthorizedFu(oldSc.study), privateForbiddenFu(oldSc.study))
    .dmap(_.noCache)

  private[controllers] def getJsonData(sc: WithChapter, withChapters: Boolean)(using
      ctx: Context
  ): Fu[(WithChapter, JsData)] =
    for
      (studyFromDb, chapter) <- env.study.api.maybeResetAndGetChapter(sc.study, sc.chapter)
      study <- env.relay.api.reconfigureStudy(studyFromDb, chapter)
      previews <- withChapters.optionFu(env.study.preview.jsonList(study.id))
      _ <- env.user.lightUserApi.preloadMany(study.members.ids.toList)
      fedNames <- env.study.preview.federations.get(sc.study.id)
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
      env.study.api.byIdWithChapter(id).flatMap(showQuery)

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
                else showQuery(none)
          case sc => showQuery(sc)

  def chapterConfig(id: StudyId, chapterId: StudyChapterId) = Open:
    Found(env.study.chapterRepo.byIdAndStudy(chapterId, id)): chapter =>
      Ok(env.study.jsonView.chapterConfig(chapter))

  private[controllers] def chatOf(study: lila.study.Study)(using ctx: Context) = {
    ctx.kid.no && ctx.noBot // no public chats for kids and bots
  }.optionFu:
    env.chat.api.userChat
      .findMine(study.id.into(ChatId))
      .mon(_.chat.fetch("study"))

  def createAs = AuthBody { ctx ?=> me ?=>
    bindForm(StudyForm.importGame.form)(
      _ => Redirect(routes.Study.byOwnerDefault(me.username)),
      data =>
        for
          owner <- env.study.api.recentByOwnerWithChapterCount(me, 50)
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
        _ <- env.study.api.delete(study)
      yield round match
        case None => Redirect(routes.Study.mine(Order.hot))
        case Some(tour) => Redirect(routes.RelayTour.show(tour.slug, tour.id))
  }

  def apiChapterDelete(id: StudyId, chapterId: StudyChapterId) = ScopedBody(_.Study.Write) { _ ?=> me ?=>
    Found(env.study.api.byIdAndOwnerOrAdmin(id, me)): study =>
      env.study.api.deleteChapter(study.id, chapterId)(Who(me.userId, Sri("api"))).inject(NoContent)
  }

  def clearChat(id: StudyId) = Auth { _ ?=> me ?=>
    env.study.api
      .isOwnerOrAdmin(id, me)
      .flatMapz:
        env.chat.api.userChat.clear(id.into(ChatId))
      .inject(Redirect(routes.Study.show(id)))
  }

  private def doImportPgn(id: StudyId, data: StudyForm.importPgn.Data, sri: Sri)(
      f: (List[Chapter], Option[ErrorMsg]) => Result
  )(using ctx: Context, me: Me): Future[Result] =
    val chapterDatas = data.toChapterDatas
    limit.studyPgnImport(me, rateLimited, cost = chapterDatas.size):
      env.study.api
        .importPgns(id, chapterDatas, sticky = data.sticky, ctx.pref.showRatings)(Who(me, sri))
        .map(f.tupled)

  def importPgn(id: StudyId) = AuthBody { ctx ?=> me ?=>
    get("sri").so: sri =>
      bindForm(StudyForm.importPgn.form)(
        doubleJsonFormError,
        data =>
          doImportPgn(id, data, Sri(sri)): (_, errors) =>
            errors.fold(NoContent)(BadRequest(_))
      )
  }

  def apiImportPgn(id: StudyId) = ScopedBody(_.Study.Write) { ctx ?=> me ?=>
    bindForm(StudyForm.importPgn.form)(
      jsonFormError,
      data =>
        doImportPgn(id, data, Sri("api")): (chapters, errors) =>
          import lila.study.ChapterPreview.json.given
          val previews = chapters.map(env.study.preview.fromChapter(_))
          JsonOk(Json.obj("chapters" -> previews, "error" -> errors))
    )
  }

  def admin(id: StudyId) = Secure(_.StudyAdmin) { ctx ?=> me ?=>
    Bus.pub(BecomeStudyAdmin(id, me))
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
                case _ => notFound

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

  private def doPgn(study: StudyModel, flags: Update[WithFlags])(using RequestHeader) =
    def makeStudySource = env.study.pgnDump.chaptersOf(study, _ => flags(requestPgnFlags))
    val pgnSource = akka.stream.scaladsl.Source.futureSource:
      if study.isRelay
      then env.relay.pgnStream.ofStudy(study).map(_ | makeStudySource)
      else fuccess(makeStudySource)
    Ok.chunked(pgnSource.throttle(20, 1.second))
      .asAttachmentStream(s"${env.study.pgnDump.filename(study)}.pgn")
      .as(pgnContentType)
      .withDateHeaders(lastModified(study.updatedAt))

  def chapterPgn(id: StudyId, chapterId: StudyChapterId) = Open:
    doChapterPgn(id, chapterId, notFound, privateUnauthorizedFu, privateForbiddenFu)

  def apiChapterPgn(id: StudyId, chapterId: StudyChapterId) =
    AnonOrScoped(_.Study.Read, _.Web.Mobile): ctx ?=>
      doChapterPgn(
        id,
        chapterId,
        fuccess(NotFound("Study or chapter not found")),
        _ => fuccess(Unauthorized("This study is now private")),
        _ => fuccess(Forbidden("This study is now private"))
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
        _.fold(studyNotFound) { case sc @ WithChapter(study, chapter) =>
          CanView(study) {
            def makeChapterPgn = env.study.pgnDump.ofChapter(study, requestPgnFlags)(chapter)
            val pgnFu =
              if study.isRelay
              then env.relay.pgnStream.ofChapter(sc).getOrElse(makeChapterPgn)
              else makeChapterPgn
            pgnFu.map: pgn =>
              Ok(pgn.toString)
                .asAttachment(s"${env.study.pgnDump.filename(study, chapter)}.pgn")
                .as(pgnContentType)
          }(studyUnauthorized(study), studyForbidden(study))
        }

  def exportPgn(username: UserStr) = OpenOrScoped(_.Study.Read, _.Web.Mobile): ctx ?=>
    val name =
      if username.value == "me"
      then ctx.me.fold(UserName("me"))(_.username)
      else username.into(UserName)
    val userId = name.id
    val isMe = ctx.me.exists(_.is(userId))
    val makeStream = env.study.studyRepo
      .sourceByOwner(userId, isMe)
      .flatMapConcat(env.study.pgnDump.chaptersOf(_, _ => requestPgnFlags))
      .throttle(16, 1.second)
    apiC.GlobalConcurrencyLimitPerIpAndUserOption(userId.some)(makeStream): source =>
      Ok.chunked(source)
        .asAttachmentStream(s"${name}-${if isMe then "all" else "public"}-studies.pgn")
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
                .asAttachmentStream(s"${env.study.pgnDump.filename(study, chapter)}.gif")
                .as("image/gif")
            .recover { case lila.core.lilaism.LilaInvalid(msg) =>
              BadRequest(msg)
            }
        }(privateUnauthorizedFu(study), privateForbiddenFu(study))

  def apiChapterTagsUpdate(studyId: StudyId, chapterId: StudyChapterId) =
    AuthOrScopedBody(_.Study.Write) { _ ?=> _ ?=>
      bindForm(StudyForm.chapterTagsForm)(
        jsonFormError,
        pgn => env.study.api.updateChapterTags(studyId, chapterId, pgn).inject(NoContent)
      )
    }

  def topicAutocomplete = Anon:
    get("term").filter(_.nonEmpty) match
      case None => BadRequest("No search term provided")
      case Some(term) =>
        import lila.common.Json.given
        env.study.topicApi.findLike(term, getUserStr("user").map(_.id)).map { JsonOk(_) }

  def topics = OpenOrScoped():
    for
      popular <- env.study.topicApi.popular(50)
      ofUser = ctx.userId.ifTrue(ctx.isWebAuth || ctx.oauth.exists(_.has(_.Study.Read)))
      mine <- ofUser.traverse(env.study.topicApi.userTopics)
      result <- negotiate(
        Ok.page(views.study.list.topic.index(popular, mine, mine.map(StudyForm.topicsForm))),
        Ok(Json.obj("popular" -> popular).add("mine" -> mine))
      )
    yield result

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

  def privateUnauthorizedJson = Unauthorized(jsonError("This study is now private"))
  def privateUnauthorizedFu(study: StudyModel)(using Context) = negotiate(
    Unauthorized.page(views.study.privateStudy(study)),
    privateUnauthorizedJson
  )

  def privateForbiddenJson = forbiddenJson("This study is now private")
  def privateForbiddenFu(study: StudyModel)(using Context) = negotiate(
    Forbidden.page(views.study.privateStudy(study)),
    privateForbiddenJson
  )

  def CanView(study: StudyModel, userSelection: Option[Settings.UserSelection] = none)(
      f: => Fu[Result]
  )(unauthorized: => Fu[Result], forbidden: => Fu[Result])(using me: Option[Me]): Fu[Result] =
    def withUserSelection =
      if userSelection.forall(Settings.UserSelection.allows(_, study, me.map(_.userId))) then f
      else forbidden
    me match
      case _ if !study.isPrivate => withUserSelection
      case None => unauthorized
      case Some(me) if study.members.contains(me.value) => withUserSelection
      case _ => forbidden

  private val streamerCache =
    env.memo.cacheApi[StudyId, List[UserId]](64, "study.streamers"):
      _.expireAfterWrite(10.seconds).buildAsyncFuture: studyId =>
        env.study.findConnectedUsersIn(studyId)(env.streamer.liveStreamApi.streamerUserIds)

  def glyphs(lang: String) = Anon:
    Found(play.api.i18n.Lang.get(lang)): lang =>
      JsonOk:
        lila.study.JsonView.glyphs(using env.translator.to(lang))
      .headerCacheSeconds(3600)
