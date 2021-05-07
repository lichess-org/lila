package views.html.video

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator

import controllers.routes

object bits {

  private[video] def card(vv: lila.video.VideoView, control: lila.video.UserControl) =
    a(cls := "card paginated", href := s"${routes.Video.show(vv.video.id)}?${control.queryStringUnlessBot}")(
      vv.view option span(cls := "view")("watched"),
      span(cls := "duration")(vv.video.durationString),
      span(cls := "img", style := s"background-image: url(${vv.video.thumbnail})"),
      span(cls := "info")(
        span(cls := "title")(vv.video.title)
      ),
      span(cls := "reveal")(
        span(cls := "full-title")(vv.video.title),
        span(cls := "author")(vv.video.author),
        span(cls := "target")(vv.video.targets.map(lila.video.Target.name).mkString(", ")),
        span(cls := "tags")(
          vv.video.tags.map { tag =>
            span(dataIcon := "o")(tag.capitalize)
          }
        )
      )
    )

  def author(name: String, videos: Paginator[lila.video.VideoView], control: lila.video.UserControl)(implicit
      ctx: Context
  ) =
    layout(
      title = s"$name • Free Chess Videos",
      control = control
    )(
      div(cls := "box__top")(
        h1(
          a(cls := "is4 text", dataIcon := "i", href := s"${routes.Video.index}?${control.queryString}"),
          name
        ),
        span(
          pluralize("video", videos.nbResults),
          " found"
        )
      ),
      div(cls := "list infinite-scroll box__pad")(
        videos.currentPageResults.map { card(_, control) },
        pagerNext(videos, np => s"${routes.Video.author(name)}?${control.queryString}&page=$np")
      )
    )

  def notFound(control: lila.video.UserControl)(implicit ctx: Context) =
    layout(title = "Video not found", control = control)(
      div(cls := "content_box_top")(
        a(cls := "is4 text lichess_title", dataIcon := "i", href := routes.Video.index)("Video library")
      ),
      div(cls := "not_found")(
        h1("Video Not Found!"),
        br,
        br,
        a(cls := "big button text", dataIcon := "i", href := routes.Video.index)(
          "Return to the video library"
        )
      )
    )

  def searchForm(query: Option[String])(implicit ctx: Context) =
    form(cls := "search", method := "GET", action := routes.Video.index)(
      input(placeholder := trans.search.search.txt(), tpe := "text", name := "q", value := query)
    )

  def tags(ts: List[lila.video.TagNb], control: lila.video.UserControl)(implicit ctx: Context) =
    layout(title = s"Tags • Free Chess Videos", control = control)(
      div(cls := "box__top")(
        h1(cls := "lichess_title")(
          a(cls := "text", dataIcon := "i", href := s"${routes.Video.index}?${control.queryString}")(
            "All ",
            ts.size,
            " video tags"
          )
        )
      ),
      div(cls := "tag-list box__pad")(
        ts.sortBy(_.tag).map { t =>
          a(cls := "tag", href := s"${routes.Video.index}?tags=${t.tag}")(
            t.tag.capitalize,
            em(t.nb)
          )
        }
      )
    )
}
