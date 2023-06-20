package controllers

import play.api.libs.json.*
import play.api.mvc.*
import scala.util.chaining.*

import lila.app.{ given, * }
import lila.common.paginator.{ Paginator, PaginatorJson }
import lila.common.{ HTTPRequest, IpAddress }
import lila.study.actorApi.Who
import lila.study.JsonView.JsData
import lila.study.Study.WithChapter
import lila.study.{ Order, StudyForm, Study as StudyModel }
import lila.tree.Node.partitionTreeJsonWriter
import views.*
import lila.analyse.Analysis

final class Study(
    env: Env,
    editorC: => Editor,
    userAnalysisC: => UserAnalysis,
    apiC: => Api,
    prismicC: Prismic
) extends LilaController(env):

  def search(text: String, page: Int) = OpenBody:
    Reasonable(page):
      if text.trim.isEmpty then
        env.study.pager.all(Order.default, page) flatMap { pag =>
          preloadMembers(pag) >> negotiate(
            html = Ok(html.study.list.all(pag, Order.default)),
            api = _ => apiStudies(pag)
          )
        }
      else
        env.studySearch(ctx.me)(text, page) flatMap { pag =>
          negotiate(
            html = Ok(html.study.list.search(pag, text)),
            api = _ => apiStudies(pag)
          )
        }

  def homeLang = LangPage(routes.Study.allDefault())(allResults(Order.Hot, 1))

  def allDefault(page: Int) = all(Order.Hot, page)

  def all(order: Order, page: Int) = Open:
    allResults(order, page)

  private def allResults(order: Order, page: Int)(using ctx: WebContext) =
    Reasonable(page) {
      order match
        case order if !Order.withoutSelector.contains(order) =>
          Redirect(routes.Study.allDefault(page))
        case order =>
          env.study.pager.all(order, page) flatMap { pag =>
            preloadMembers(pag) >> negotiate(
              html = Ok(html.study.list.all(pag, order)),
              api = _ => apiStudies(pag)
            )
          }
    }

  def byOwnerDefault(username: UserStr, page: Int) = byOwner(username, Order.default, page)

  def byOwner(username: UserStr, order: Order, page: Int) = Open:
    env.user.repo
      .byId(username)
      .flatMapz: owner =>
        env.study.pager
          .byOwner(owner, order, page)
          .flatMap: pag =>
            preloadMembers(pag) >> negotiate(
              html = Ok(html.study.list.byOwner(pag, order, owner)),
              api = _ => apiStudies(pag)
            )

  def mine(order: Order, page: Int) = Auth { ctx ?=> me ?=>
    env.study.pager.mine(order, page) flatMap { pag =>
      preloadMembers(pag) >> negotiate(
        html = env.study.topicApi.userTopics(me) map { topics =>
          Ok(html.study.list.mine(pag, order, topics))
        },
        api = _ => apiStudies(pag)
      )
    }
  }

  def minePublic(order: Order, page: Int) = Auth { ctx ?=> me ?=>
    env.study.pager.minePublic(order, page) flatMap { pag =>
      preloadMembers(pag) >> negotiate(
        html = html.study.list.minePublic(pag, order),
        api = _ => apiStudies(pag)
      )
    }
  }

  def minePrivate(order: Order, page: Int) = Auth { ctx ?=> me ?=>
    env.study.pager.minePrivate(order, page) flatMap { pag =>
      preloadMembers(pag) >> negotiate(
        html = html.study.list.minePrivate(pag, order),
        api = _ => apiStudies(pag)
      )
    }
  }

  def mineMember(order: Order, page: Int) = Auth { ctx ?=> me ?=>
    env.study.pager.mineMember(order, page) flatMap { pag =>
      preloadMembers(pag) >> negotiate(
        html = env.study.topicApi.userTopics(me) map {
          html.study.list.mineMember(pag, order, _)
        },
        api = _ => apiStudies(pag)
      )
    }
  }

  def mineLikes(order: Order, page: Int) = Auth { ctx ?=> me ?=>
    env.study.pager.mineLikes(order, page) flatMap { pag =>
      preloadMembers(pag) >> negotiate(
        html = html.study.list.mineLikes(pag, order),
        api = _ => apiStudies(pag)
      )
    }
  }

  def byTopic(name: String, order: Order, page: Int) = Open:
    lila.study.StudyTopic fromStr name match
      case None => notFound
      case Some(topic) =>
        env.study.pager.byTopic(topic, order, page) zip
          ctx.me.so(u => env.study.topicApi.userTopics(u) dmap some) flatMap { (pag, topics) =>
            preloadMembers(pag) inject Ok(html.study.topic.show(topic, pag, order, topics))
          }

  private def preloadMembers(pag: Paginator[StudyModel.WithChaptersAndLiked]) =
    env.user.lightUserApi.preloadMany(
      pag.currentPageResults.view
        .flatMap(_.study.members.members.values take StudyModel.previewNbMembers)
        .map(_.id)
        .toSeq
    )

  private def apiStudies(pager: Paginator[StudyModel.WithChaptersAndLiked]) =
    given Writes[StudyModel.WithChaptersAndLiked] = Writes[StudyModel.WithChaptersAndLiked]: s =>
      env.study.jsonView.pagerData(s)
    Ok(Json.obj("paginator" -> PaginatorJson(pager)))

  private def orRelay(id: StudyId, chapterId: Option[StudyChapterId] = None)(
      f: => Fu[Result]
  )(using ctx: WebContext): Fu[Result] =
    if HTTPRequest isRedirectable ctx.req
    then
      env.relay.api.getOngoing(id into RelayRoundId) flatMap {
        _.fold(f): rt =>
          Redirect(chapterId.fold(rt.path)(rt.path))
      }
    else f

  private def showQuery(query: Fu[Option[WithChapter]])(using ctx: WebContext): Fu[Result] =
    OptionFuResult(query): oldSc =>
      CanView(oldSc.study) {
        for
          (sc, data) <- getJsonData(oldSc)
          res <- negotiate(
            html =
              for
                chat      <- chatOf(sc.study)
                sVersion  <- env.study.version(sc.study.id)
                streamers <- streamersOf(sc.study)
              yield Ok(html.study.show(sc.study, data, chat, sVersion, streamers))
                .withCanonical(routes.Study.chapter(sc.study.id, sc.chapter.id))
                .enableSharedArrayBuffer,
            api = _ =>
              chatOf(sc.study).map: chatOpt =>
                Ok:
                  Json.obj(
                    "study" -> data.study.add("chat" -> chatOpt.map { c =>
                      lila.chat.JsonView.mobile(
                        chat = c.chat,
                        writeable = ctx.userId.so(sc.study.canChat)
                      )
                    }),
                    "analysis" -> data.analysis
                  )
          )
        yield res
      }(privateUnauthorizedFu(oldSc.study), privateForbiddenFu(oldSc.study))
    .dmap(_.noCache)

  private[controllers] def getJsonData(sc: WithChapter)(using ctx: WebContext): Fu[(WithChapter, JsData)] =
    for
      chapters                <- env.study.chapterRepo.orderedMetadataByStudy(sc.study.id)
      (study, resetToChapter) <- env.study.api.resetIfOld(sc.study, chapters)
      chapter = resetToChapter | sc.chapter
      _ <- env.user.lightUserApi preloadMany study.members.ids.toList
      pov = userAnalysisC.makePov(chapter.root.fen.some, chapter.setup.variant)
      analysis <- chapter.serverEval.exists(_.done) so env.analyse.analyser.byId(chapter.id into Analysis.Id)
      division = analysis.isDefined option env.study.serverEvalMerger.divisionOf(chapter)
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
      studyJson <- env.study.jsonView(study, chapters, chapter, ctx.me)
    yield WithChapter(study, chapter) -> JsData(
      study = studyJson,
      analysis = baseData
        .add(
          "treeParts" -> partitionTreeJsonWriter.writes {
            lila.study.TreeBuilder(chapter.root, chapter.setup.variant)
          }.some
        )
        .add("analysis" -> analysis.map { lila.study.ServerEval.toJson(chapter, _) })
    )

  def show(id: StudyId) = Open:
    orRelay(id):
      showQuery(env.study.api byIdWithChapter id)

  def chapter(id: StudyId, chapterId: StudyChapterId) =
    Open:
      orRelay(id, chapterId.some):
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

  def chapterMeta(id: StudyId, chapterId: StudyChapterId) = Open:
    env.study.chapterRepo.byId(chapterId).map {
      _.filter(_.studyId == id) so { chapter =>
        Ok(env.study.jsonView.chapterConfig(chapter))
      }
    }

  private[controllers] def chatOf(study: lila.study.Study)(using ctx: WebContext) = {
    ctx.noKid && ctx.noBot &&                    // no public chats for kids and bots
    ctx.me.fold(true)(env.chat.panic.allowed(_)) // anon can see public chats
  } so env.chat.api.userChat
    .findMine(study.id into ChatId)
    .dmap(some)
    .mon(_.chat.fetch("study"))

  def createAs = AuthBody { ctx ?=> me ?=>
    StudyForm.importGame.form
      .bindFromRequest()
      .fold(
        _ => Redirect(routes.Study.byOwnerDefault(me.username)),
        data =>
          for
            owner   <- env.study.api.recentByOwnerWithChapterCount(me, 50)
            contrib <- env.study.api.recentByContributorWithChapterCount(me, 50)
            res <-
              if owner.isEmpty && contrib.isEmpty then createStudy(data, me)
              else
                val back = HTTPRequest.referer(ctx.req) orElse
                  data.fen.map(fen => editorC.editorUrl(fen, data.variant | chess.variant.Variant.default))
                Ok(html.study.create(data, owner, contrib, back)).toFuccess
          yield res
      )
  }

  def create = AuthBody { ctx ?=> me ?=>
    StudyForm.importGame.form
      .bindFromRequest()
      .fold(
        _ => Redirect(routes.Study.byOwnerDefault(me.username)),
        data => createStudy(data, me)
      )
  }

  private def createStudy(data: StudyForm.importGame.Data, me: lila.user.User)(using ctx: WebContext) =
    env.study.api.importGame(lila.study.StudyMaker.ImportGame(data), me, ctx.pref.showRatings) flatMap {
      _.fold(notFound): sc =>
        Redirect(routes.Study.chapter(sc.study.id, sc.chapter.id))
    }

  def delete(id: StudyId) = Auth { _ ?=> me ?=>
    env.study.api.byIdAndOwnerOrAdmin(id, me) flatMapz { study =>
      env.study.api.delete(study) >> env.relay.api.deleteRound(id into RelayRoundId).map {
        case None       => Redirect(routes.Study.mine("hot"))
        case Some(tour) => Redirect(routes.RelayTour.redirectOrApiTour(tour.slug, tour.id.value))
      }
    }
  }

  def clearChat(id: StudyId) = Auth { _ ?=> me ?=>
    env.study.api.isOwnerOrAdmin(id, me) flatMapz {
      env.chat.api.userChat.clear(id into ChatId)
    } inject Redirect(routes.Study.show(id))
  }

  def importPgn(id: StudyId) = AuthBody { ctx ?=> me ?=>
    get("sri") so { sri =>
      StudyForm.importPgn.form
        .bindFromRequest()
        .fold(
          jsonFormError,
          data =>
            env.study.api.importPgns(
              id,
              data.toChapterDatas,
              sticky = data.sticky,
              ctx.pref.showRatings
            )(Who(me, lila.socket.Socket.Sri(sri)))
        )
    }
  }

  def admin(id: StudyId) = Secure(_.StudyAdmin) { ctx ?=> me ?=>
    env.study.api
      .adminInvite(id)
      .inject:
        if HTTPRequest.isXhr(ctx.req) then NoContent else Redirect(routes.Study.show(id))
  }

  def embed(id: StudyId, chapterId: StudyChapterId) = Anon:
    val studyWithChapter =
      if (chapterId.value == "autochap") env.study.api.byIdWithChapter(id)
      else env.study.api.byIdWithChapterOrFallback(id, chapterId)
    studyWithChapter.map(_.filterNot(_.study.isPrivate)) flatMap {
      _.fold(embedNotFound) { case WithChapter(study, chapter) =>
        for
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
            owner = false
          )
          analysis = baseData ++ Json.obj(
            "treeParts" -> partitionTreeJsonWriter.writes {
              lila.study.TreeBuilder.makeRoot(chapter.root, setup.variant)
            }
          )
          data = lila.study.JsonView.JsData(study = studyJson, analysis = analysis)
          result <- negotiate(
            html = Ok(html.study.embed(study, chapter, chapters, data)),
            api = _ => Ok(Json.obj("study" -> data.study, "analysis" -> data.analysis))
          )
        yield result
      }
    } dmap (_.noCache)

  private def embedNotFound(using RequestHeader): Fu[Result] =
    NotFound(html.study.embed.notFound)

  def cloneStudy(id: StudyId) = Auth { ctx ?=> _ ?=>
    OptionFuResult(env.study.api.byId(id)) { study =>
      CanView(study) {
        Ok(html.study.clone(study))
      }(privateUnauthorizedFu(study), privateForbiddenFu(study))
    }
  }

  private val CloneLimitPerUser = lila.memo.RateLimit[UserId](
    credits = 10 * 3,
    duration = 24.hour,
    key = "study.clone.user"
  )

  private val CloneLimitPerIP = lila.memo.RateLimit[IpAddress](
    credits = 20 * 3,
    duration = 24.hour,
    key = "study.clone.ip"
  )

  def cloneApply(id: StudyId) = Auth { ctx ?=> me ?=>
    val cost = if (isGranted(_.Coach) || me.hasTitle) 1 else 3
    CloneLimitPerUser(me, rateLimitedFu, cost = cost):
      CloneLimitPerIP(ctx.ip, rateLimitedFu, cost = cost):
        OptionFuResult(env.study.api.byId(id)) { prev =>
          CanView(prev) {
            env.study.api.clone(me, prev) map { study =>
              Redirect(routes.Study.show((study | prev).id))
            }
          }(privateUnauthorizedFu(prev), privateForbiddenFu(prev))
        }
  }

  private val PgnRateLimitPerIp = lila.memo.RateLimit[IpAddress](
    credits = 31,
    duration = 1.minute,
    key = "export.study.pgn.ip"
  )

  def pgn(id: StudyId) = Open:
    PgnRateLimitPerIp(ctx.ip, rateLimitedFu, msg = id):
      OptionFuResult(env.study.api byId id): study =>
        CanView(study) {
          doPgn(study)
        }(privateUnauthorizedFu(study), privateForbiddenFu(study))

  def apiPgn(id: StudyId) = AnonOrScoped(_.Study.Read): ctx ?=>
    env.study.api.byId(id).flatMap {
      _.fold(studyNotFoundText.toFuccess): study =>
        if ctx.req.method == "HEAD" then Ok.withDateHeaders(studyLastModified(study))
        else
          PgnRateLimitPerIp[Fu[Result]](req.ipAddress, rateLimited, msg = id):
            CanView(study) {
              doPgn(study)
            }(privateUnauthorizedText, privateForbiddenText)
    }

  private def doPgn(study: StudyModel)(using RequestHeader) =
    Ok.chunked(env.study.pgnDump.chaptersOf(study, requestPgnFlags).throttle(16, 1.second))
      .pipe(asAttachmentStream(s"${env.study.pgnDump filename study}.pgn"))
      .as(pgnContentType)
      .withDateHeaders(studyLastModified(study))

  private def studyLastModified(s: StudyModel) = LAST_MODIFIED -> s.updatedAt.atZone(utcZone)

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
  )(using ctx: AnyContext) =
    env.study.api.byIdWithChapter(id, chapterId) flatMap {
      _.fold(studyNotFound) { case WithChapter(study, chapter) =>
        CanView(study) {
          env.study.pgnDump.ofChapter(study, requestPgnFlags)(chapter) map { pgn =>
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
    val isMe   = ctx.me.exists(_ is userId)
    val makeStream = env.study.studyRepo
      .sourceByOwner(userId, isMe)
      .flatMapConcat(env.study.pgnDump.chaptersOf(_, requestPgnFlags))
      .throttle(16, 1.second)
      .withAttributes:
        akka.stream.ActorAttributes.supervisionStrategy(akka.stream.Supervision.resumingDecider)
    apiC.GlobalConcurrencyLimitPerIpAndUserOption(userId.some)(makeStream): source =>
      Ok.chunked(source)
        .pipe(asAttachmentStream(s"${name}-${if (isMe) "all" else "public"}-studies.pgn"))
        .as(pgnContentType)

  def apiListByOwner(username: UserStr) = OpenOrScoped(_.Study.Read): ctx ?=>
    val isMe = ctx is username
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
    env.study.api.byIdWithChapter(id, chapterId) flatMap {
      _.fold(notFound) { case WithChapter(study, chapter) =>
        CanView(study) {
          env.study.gifExport.ofChapter(chapter, theme, piece) map { stream =>
            Ok.chunked(stream)
              .pipe(asAttachmentStream(s"${env.study.pgnDump.filename(study, chapter)}.gif"))
              .as("image/gif")
          } recover { case lila.base.LilaInvalid(msg) =>
            BadRequest(msg)
          }
        }(privateUnauthorizedFu(study), privateForbiddenFu(study))
      }
    }

  def multiBoard(id: StudyId, page: Int) = Open:
    OptionFuResult(env.study.api byId id): study =>
      CanView(study) {
        env.study.multiBoard.json(study.id, page, getBool("playing")) map JsonOk
      }(privateUnauthorizedJson, privateForbiddenJson)

  def topicAutocomplete = Anon:
    get("term").filter(_.nonEmpty) match
      case None => BadRequest("No search term provided")
      case Some(term) =>
        import lila.common.Json.given
        env.study.topicApi.findLike(term, getUserStr("user").map(_.id)) map { JsonOk(_) }

  def topics = Open:
    env.study.topicApi.popular(50) zip
      ctx.me.so(me => env.study.topicApi.userTopics(me) dmap some) map { (popular, mine) =>
        val form = mine map StudyForm.topicsForm
        Ok(html.study.topic.index(popular, mine, form))
      }

  def setTopics = AuthBody { ctx ?=> me ?=>
    StudyForm.topicsForm
      .bindFromRequest()
      .fold(
        _ => Redirect(routes.Study.topics),
        topics =>
          env.study.topicApi.userTopics(me, topics) inject
            Redirect(routes.Study.topics)
      )
  }

  def staffPicks = Open:
    pageHit
    OptionOk(prismicC getBookmark "studies-staff-picks") { case (doc, resolver) =>
      html.study.list.staffPicks(doc, resolver)
    }

  def privateUnauthorizedText = Unauthorized("This study is now private")
  def privateUnauthorizedJson = Unauthorized(jsonError("This study is now private"))
  def privateUnauthorizedFu(study: StudyModel)(using WebContext) =
    negotiate(
      html = Unauthorized(html.site.message.privateStudy(study)),
      api = _ => privateUnauthorizedJson
    )

  def privateForbiddenText = Forbidden("This study is now private")
  def privateForbiddenJson = Forbidden(jsonError("This study is now private"))
  def privateForbiddenFu(study: StudyModel)(using WebContext) =
    negotiate(
      html = Forbidden(html.site.message.privateStudy(study)),
      api = _ => privateForbiddenJson
    )

  def studyNotFoundText = NotFound("Study or chapter not found")
  def studyNotFoundJson = NotFound(jsonError("Study or chapter not found"))

  def CanView(study: StudyModel)(
      f: => Fu[Result]
  )(unauthorized: => Fu[Result], forbidden: => Fu[Result])(using me: Option[Me]): Fu[Result] =
    me match
      case _ if !study.isPrivate                       => f
      case None                                        => unauthorized
      case Some(me) if study.members.contains(me.user) => f
      case _                                           => forbidden

  private[controllers] def streamersOf(study: StudyModel) = streamerCache get study.id

  private val streamerCache =
    env.memo.cacheApi[StudyId, List[UserId]](64, "study.streamers"):
      _.refreshAfterWrite(15.seconds)
        .maximumSize(512)
        .buildAsyncFuture: studyId =>
          env.study.studyRepo.membersById(studyId) flatMap {
            _.map(_.members).filter(_.nonEmpty) so { members =>
              env.streamer.liveStreamApi.all.flatMap:
                _.streams
                  .filter: s =>
                    members.exists(m => s is m._2.id)
                  .map: stream =>
                    env.study.isConnected(studyId, stream.streamer.userId) map {
                      _ option stream.streamer.userId
                    }
                  .parallel
                  .dmap(_.flatten)
            }
          }

  def glyphs(lang: String) = Anon:
    play.api.i18n.Lang.get(lang) so { lang =>
      JsonOk:
        lila.study.JsonView.glyphs(lang)
      .withHeaders(CACHE_CONTROL -> "max-age=3600")
    }
