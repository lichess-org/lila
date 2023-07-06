package views.html.video

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.paginator.Paginator

import controllers.routes

object index:

  def apply(videos: Paginator[lila.video.VideoView], count: Long, control: lila.video.UserControl)(using
      ctx: PageContext
  ) =

    val tagString =
      s"${if control.filter.tags.nonEmpty then control.filter.tags.mkString(" + ") + " • " else ""}"

    layout(
      title = s"${tagString}Free Chess Videos",
      openGraph = lila.app.ui
        .OpenGraph(
          title = s"${tagString}free, carefully curated chess videos",
          description = s"${videos.nbResults} curated chess videos${
              if tagString.nonEmpty then " matching the tags " + tagString
              else " • "
            }free for all",
          url = s"$netBaseUrl${routes.Video.index}?${control.queryString}"
        )
        .some,
      control = control
    )(
      boxTop(
        h1(
          if control.filter.tags.nonEmpty then frag(pluralize("video", videos.nbResults), " found")
          else "Chess videos"
        ),
        bits.searchForm(control.query)
      ),
      control.filter.tags.isEmpty option p(cls := "explain box__pad")(
        "All videos are free for everyone.",
        br,
        "Click one or many tags on the left to filter.",
        br,
        "We have carefully selected ",
        strong(count),
        " videos so far!"
      ),
      div(cls := "list box__pad infinite-scroll")(
        videos.currentPageResults.map { bits.card(_, control) },
        videos.currentPageResults.sizeIs < 4 option div(cls := s"not_much nb_${videos.nbResults}")(
          if videos.currentPageResults.isEmpty then "No videos for these tags:"
          else "That's all we got for these tags:",
          control.filter.tags.map { tag =>
            a(cls := "tag", dataIcon := licon.Tag, href := s"${routes.Video.index}?tags=$tag")(tag.capitalize)
          },
          br,
          br,
          a(href := routes.Video.index, cls := "button")("Clear search")
        ),
        pagerNext(videos, np => s"${routes.Video.index}?${control.queryString}&page=$np")
      )
    )
