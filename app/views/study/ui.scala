package views.study

import play.api.libs.json.Json
import chess.format.pgn.PgnStr

import lila.app.templating.Environment.{ *, given }
import lila.core.study.IdName
import lila.core.socket.SocketVersion
import lila.common.Json.given

lazy val bits = lila.study.ui.StudyBits(helpers)
lazy val ui   = lila.study.ui.StudyUi(helpers, bits)

def streamers(streamers: List[UserId])(using Translate) =
  views.streamer.bits.contextual(streamers).map(_(cls := "none"))

def jsI18n()(using Translate) =
  views.board.userAnalysisI18n(withAdvantageChart = true) ++
    i18nJsObject(bits.i18nKeys ++ bits.gamebookPlayKeys)

def embedJsI18n(chapter: lila.study.Chapter)(using Translate) =
  views.board.userAnalysisI18n() ++ chapter.isGamebook.so(i18nJsObject(bits.gamebookPlayKeys))

def clone(s: lila.study.Study)(using PageContext) =
  views.site.message(title = s"Clone ${s.name}", icon = Icon.StudyBoard.some)(ui.clone(s))

def create(
    data: lila.study.StudyForm.importGame.Data,
    owner: List[(IdName, Int)],
    contrib: List[(IdName, Int)],
    backUrl: Option[String]
)(using PageContext) =
  views.site.message(
    title = trans.site.toStudy.txt(),
    icon = Some(Icon.StudyBoard),
    back = backUrl,
    moreCss = cssTag("study.create").some
  )(ui.create(data, owner, contrib, backUrl))

def show(
    s: lila.study.Study,
    data: lila.study.JsonView.JsData,
    chatOption: Option[lila.chat.UserChat.Mine],
    socketVersion: SocketVersion,
    streamers: List[UserId]
)(using ctx: PageContext) =
  views.base.layout(
    title = s.name.value,
    moreCss = cssTag("analyse.study"),
    modules = analyseNvuiTag,
    pageModule = PageModule(
      "analyse.study",
      Json.obj(
        "study" -> data.study
          .add("admin", isGranted(_.StudyAdmin))
          .add("showRatings", ctx.pref.showRatings),
        "data"     -> data.analysis,
        "i18n"     -> jsI18n(),
        "tagTypes" -> lila.study.PgnTags.typesToString,
        "userId"   -> ctx.userId,
        "chat" -> chatOption.map: c =>
          views.chat.json(
            c.chat,
            c.lines,
            name = trans.site.chatRoom.txt(),
            timeout = c.timeout,
            writeable = ctx.userId.exists(s.canChat),
            public = true,
            resourceId = lila.chat.Chat.ResourceId(s"study/${c.chat.id}"),
            palantir = ctx.userId.exists(s.isMember),
            localMod = ctx.userId.exists(s.canContribute)
          ),
        "socketUrl"     -> socketUrl(s.id),
        "socketVersion" -> socketVersion
      ) ++ views.board.bits.explorerAndCevalConfig
    ).some,
    robots = s.isPublic,
    zoomable = true,
    csp = analysisCsp.withPeer.withExternalAnalysisApis.some,
    openGraph = OpenGraph(
      title = s.name.value,
      url = s"$netBaseUrl${routes.Study.show(s.id).url}",
      description = s"A chess study by ${titleNameOrId(s.ownerId)}"
    ).some
  ):
    frag(
      main(cls := "analyse"),
      views.streamer.bits.contextual(streamers).map(_(cls := "none"))
    )

def socketUrl(id: StudyId) = s"/study/$id/socket/v$apiVersion"

object embed:

  def apply(s: lila.study.Study, chapter: lila.study.Chapter, pgn: PgnStr)(using ctx: EmbedContext) =
    import views.analyse.embed.*
    val canGetPgn = s.settings.shareable == lila.study.Settings.UserSelection.Everyone
    views.base.embed(
      title = s"${s.name} ${chapter.name}",
      cssModule = "lpv.embed",
      modules = EsmInit("site.lpvEmbed")
    )(
      div(cls := "is2d")(div(pgn)),
      lpvJs:
        lpvConfig(orientation = none, getPgn = canGetPgn) ++ Json
          .obj()
          .add(
            "gamebook" -> chapter.isGamebook
              .option(Json.obj("url" -> routes.Study.chapter(s.id, chapter.id).url))
          )
    )

  def notFound(using EmbedContext) =
    views.base.embed(title = s"404 - ${trans.study.studyNotFound.txt()}", cssModule = "lpv.embed"):
      div(cls := "not-found")(h1(trans.study.studyNotFound()))
