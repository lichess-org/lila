package views.study

import chess.format.pgn.PgnStr
import play.api.libs.json.Json

import lila.app.UiEnv.{ *, given }
import lila.common.Json.given

lazy val bits = lila.study.ui.StudyBits(helpers)
lazy val ui = lila.study.ui.StudyUi(helpers)(analyseEndpoints)
lazy val list = lila.study.ui.StudyListUi(helpers, bits)

def chatJson(s: lila.study.Study, c: lila.chat.UserChat.Mine)(using ctx: Context) =
  views.chat.json(
    c.chat,
    c.lines,
    name = trans.site.chatRoom.txt(),
    timeout = c.timeout,
    writeable = ctx.userId.exists(s.canChat),
    public = true,
    resource = lila.core.chat.PublicSource.Study(s.id),
    localMod = ctx.userId.exists(s.canContribute)
  )

def embed(s: lila.study.Study, chapterId: StudyChapterId, pgn: PgnStr)(using ctx: EmbedContext) =
  val canGetPgn = s.settings.shareable == lila.study.Settings.UserSelection.Everyone
  val isGamebook = pgn.value.contains("""[ChapterMode "gamebook"]""")
  views.analyse.embed.lpv(
    pgn,
    canGetPgn,
    title = s.name.value,
    isGamebook.so:
      Json.obj("gamebook" -> Json.obj("url" -> routes.Study.chapter(s.id, chapterId).url))
  )

def embedNotFound(using EmbedContext) =
  views.base.embed
    .minimal(title = s"404 - ${trans.study.studyNotFound.txt()}", cssKeys = List("bits.lpv.embed")):
      div(cls := "not-found")(h1(trans.study.studyNotFound()))
