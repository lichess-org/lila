package views.study

import chess.format.pgn.PgnStr
import play.api.libs.json.Json

import lila.app.UiEnv.{ *, given }
import lila.common.Json.given
import lila.core.socket.SocketVersion
import lila.core.study.{ IdName, Order }

lazy val bits = lila.study.ui.StudyBits(helpers)
lazy val ui   = lila.study.ui.StudyUi(helpers)
lazy val list = lila.study.ui.ListUi(helpers, bits)

def staffPicks(p: lila.cms.CmsPage.Render)(using Context) =
  Page(p.title).css("analyse.study.index", "bits.page"):
    main(cls := "page-menu")(
      list.menu("staffPicks", Order.mine, Nil),
      main(cls := "page-menu__content box box-pad page"):
        views.site.page.pageContent(p)
    )

def streamers(streamers: List[UserId])(using Translate) =
  views.streamer.bits.contextual(streamers).map(_(cls := "none"))

def jsI18n()(using Translate) =
  views.userAnalysisI18n(withAdvantageChart = true) ++
    i18nJsObject(bits.i18nKeys ++ bits.gamebookPlayKeys)

def embedJsI18n(chapter: lila.study.Chapter)(using Translate) =
  views.userAnalysisI18n() ++ chapter.isGamebook.so(i18nJsObject(bits.gamebookPlayKeys))

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
    .css("analyse.study.create")(ui.create(data, owner, contrib, backUrl))

def show(
    s: lila.study.Study,
    data: lila.study.JsonView.JsData,
    chatOption: Option[lila.chat.UserChat.Mine],
    socketVersion: SocketVersion,
    streamers: List[UserId]
)(using ctx: Context) =
  Page(s.name.value)
    .css("analyse.study")
    .css(ctx.pref.hasKeyboardMove.option("keyboardMove"))
    .js(analyseNvuiTag)
    .js(
      PageModule(
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
        ) ++ views.board.explorerAndCevalConfig
      )
    )
    .robots(s.isPublic)
    .zoom
    .csp(views.analyse.ui.csp.compose(_.withPeer.withExternalAnalysisApis))
    .graph(
      title = s.name.value,
      url = s"$netBaseUrl${routes.Study.show(s.id).url}",
      description = s"A chess study by ${titleNameOrId(s.ownerId)}"
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

  def apply(s: lila.study.Study, chapter: lila.study.Chapter, pgn: PgnStr)(using ctx: EmbedContext) =
    val canGetPgn = s.settings.shareable == lila.study.Settings.UserSelection.Everyone
    views.base.embed.minimal(
      title = s"${s.name} ${chapter.name}",
      cssKeys = List("bits.lpv.embed"),
      modules = EsmInit("site.lpvEmbed")
    )(
      div(cls := "is2d")(div(pgn)),
      views.analyse.ui.embed.lpvJs(
        views.analyse.ui.embed.lpvConfig(orientation = none, getPgn = canGetPgn) ++
          chapter.isGamebook.so:
            Json.obj:
              "gamebook" -> Json.obj("url" -> routes.Study.chapter(s.id, chapter.id).url)
      )(ctx.nonce.some)
    )

  def notFound(using EmbedContext) =
    views.base.embed
      .minimal(title = s"404 - ${trans.study.studyNotFound.txt()}", cssKeys = List("bits.lpv.embed")):
        div(cls := "not-found")(h1(trans.study.studyNotFound()))
