package views.study

import chess.format.pgn.PgnStr
import play.api.libs.json.Json

import lila.app.UiEnv.{ *, given }
import lila.common.Json.given
import lila.core.socket.SocketVersion
import lila.core.study.{ IdName, StudyOrder }

lazy val bits = lila.study.ui.StudyBits(helpers)
lazy val ui = lila.study.ui.StudyUi(helpers)
lazy val list = lila.study.ui.ListUi(helpers, bits)

def staffPicks(p: lila.cms.CmsPage.Render)(using Context) =
  Page(p.title).css("analyse.study.index", "bits.page"):
    main(cls := "page-menu")(
      list.menu("staffPicks", StudyOrder.mine, Nil),
      main(cls := "page-menu__content box box-pad page"):
        views.cms.pageContent(p)
    )

def streamers(streamers: List[UserId])(using Translate) =
  views.streamer.bits.contextual(streamers).map(_(cls := "none"))

def clone(s: lila.study.Study)(using Context) =
  views.site.message(title = s"Clone ${s.name}", icon = Icon.StudyBoard.some)(ui.clone(s))

def create(
    data: lila.study.StudyForm.importGame.Data,
    owner: List[(IdName, Int)],
    contrib: List[(IdName, Int)],
    backUrl: Option[String]
)(using Context) =
  views.site
    .message(
      title = trans.site.toStudy.txt(),
      icon = Some(Icon.StudyBoard),
      back = backUrl
    )
    .css("analyse.study.create")(ui.create(data, owner, contrib))

def show(
    s: lila.study.Study,
    chapter: lila.study.Chapter,
    data: lila.study.JsonView.JsData,
    chatOption: Option[lila.chat.UserChat.Mine],
    socketVersion: SocketVersion,
    streamers: List[UserId]
)(using ctx: Context) =
  Page(s.name.value)
    .css("analyse.study")
    .css(ctx.pref.hasKeyboardMove.option("keyboardMove"))
    .css(ctx.blind.option("round.nvui"))
    .i18n(_.study)
    .i18n(_.variant)
    .i18nOpt(ctx.speechSynthesis, _.nvui)
    .i18nOpt(ctx.blind, _.keyboardMove)
    .js(analyseNvuiTag)
    .js(
      PageModule(
        "analyse.study",
        Json.obj(
          "study" -> data.study
            .add("admin", isGranted(_.StudyAdmin))
            .add("showRatings", ctx.pref.showRatings),
          "data" -> data.analysis,
          "tagTypes" -> lila.study.StudyPgnTags.typesToString,
          "userId" -> ctx.userId,
          "chat" -> chatOption.map: c =>
            views.chat.json(
              c.chat,
              c.lines,
              name = trans.site.chatRoom.txt(),
              timeout = c.timeout,
              writeable = ctx.userId.exists(s.canChat),
              public = true,
              resource = lila.core.chat.PublicSource.Study(s.id),
              voiceChat = ctx.userId.exists(s.isMember),
              localMod = ctx.userId.exists(s.canContribute)
            ),
          "socketUrl" -> socketUrl(s.id),
          "socketVersion" -> socketVersion
        ) ++ views.analyse.ui.explorerAndCevalConfig
      )
    )
    .flag(_.noRobots, !s.isPublic)
    .flag(_.zoom)
    .csp(views.analyse.ui.bits.cspExternalEngine.compose(_.withPeer.withExternalAnalysisApis))
    .graph(
      OpenGraph(
        title = s.name.value,
        url = routeUrl(routes.Study.show(s.id)),
        description = s"A chess study by ${titleNameOrId(s.ownerId)}",
        image = fenThumbnailUrl(
          chapter.root.fen.opening,
          chapter.setup.orientation.some,
          chapter.setup.variant
        ).some
      )
    ):
      frag(
        main(cls := "analyse"),
        views.streamer.bits.contextual(streamers).map(_(cls := "none"))
      )

def socketUrl(id: StudyId) = s"/study/$id/socket/v$apiVersion"

def privateStudy(study: lila.study.Study)(using Context) =
  views.site.message(
    title = s"${titleNameOrId(study.ownerId)}'s study",
    back = routes.Study.allDefault().url.some
  ):
    frag(
      "Sorry! This study is private, you cannot access it.",
      isGranted(_.StudyAdmin).option(
        postForm(action := routes.Study.admin(study.id))(
          submitButton("View as admin")(cls := "button button-red")
        )
      )
    )

object embed:

  def apply(s: lila.study.Study, chapterId: StudyChapterId, pgn: PgnStr)(using ctx: EmbedContext) =
    val canGetPgn = s.settings.shareable == lila.study.Settings.UserSelection.Everyone
    val isGamebook = pgn.value.contains("""[ChapterMode "gamebook"]""")
    views.analyse.embed.lpv(
      pgn,
      canGetPgn,
      title = s.name.value,
      isGamebook.so:
        Json.obj("gamebook" -> Json.obj("url" -> routes.Study.chapter(s.id, chapterId).url))
    )

  def notFound(using EmbedContext) =
    views.base.embed
      .minimal(title = s"404 - ${trans.study.studyNotFound.txt()}", cssKeys = List("bits.lpv.embed")):
        div(cls := "not-found")(h1(trans.study.studyNotFound()))
