package views.video

import lila.app.templating.Environment.{ *, given }

import scalalib.paginator.Paginator

private lazy val ui = lila.video.ui.VideoUi(helpers)

private def layout(
    title: String,
    control: lila.video.UserControl,
    openGraph: Option[OpenGraph] = None
)(body: Modifier*)(using PageContext) =
  views.base.layout(
    title = title,
    moreCss = cssTag("video"),
    modules = infiniteScrollEsmInit,
    wrapClass = "full-screen-force",
    openGraph = openGraph
  ):
    main(cls := "video page-menu force-ltr")(
      ui.menu(control),
      div(cls := "page-menu__content box")(body)
    )

private val titleSuffix = "• Free Chess Videos"

def index(videos: Paginator[lila.video.VideoView], count: Long, control: lila.video.UserControl)(using
    ctx: PageContext
) =
  val tagString = control.filter.tags.some.filter(_.nonEmpty).so(_.mkString(" + ") + " • ")
  layout(
    title = s"${tagString}Free Chess Videos",
    openGraph = OpenGraph(
      title = s"${tagString}free, carefully curated chess videos",
      description = s"${videos.nbResults} curated chess videos${
          if tagString.nonEmpty then " matching the tags " + tagString
          else " • "
        }free for all",
      url = s"$netBaseUrl${routes.Video.index}?${control.queryString}"
    ).some,
    control = control
  )(ui.index(videos, count, control))

def show(
    video: lila.video.Video,
    similar: Seq[lila.video.VideoView],
    control: lila.video.UserControl
)(using PageContext) =
  layout(
    title = s"${video.title} • Free Chess Videos",
    control = control,
    openGraph = OpenGraph(
      title = s"${video.title} by ${video.author}",
      description = shorten(~video.metadata.description, 152),
      url = s"$netBaseUrl${routes.Video.show(video.id)}",
      `type` = "video",
      more = video.tags.map("video:tag" -> _)
    ).some
  )(ui.show(video, similar, control))

def author(name: String, videos: Paginator[lila.video.VideoView], control: lila.video.UserControl)(using
    PageContext
) =
  layout(s"$name $titleSuffix", control)(ui.author(name, videos, control))

def notFound(control: lila.video.UserControl)(using PageContext) =
  layout("Video not found", control)(ui.notFound(control))

def tags(ts: List[lila.video.TagNb], control: lila.video.UserControl)(using PageContext) =
  layout(s"Tags $titleSuffix", control)(ui.tags(ts, control))

def search(videos: Paginator[lila.video.VideoView], control: lila.video.UserControl)(using PageContext) =
  layout(s"${control.query.getOrElse("Search")} $titleSuffix", control)(ui.search(videos, control))
