package lila.video
package ui

import scalalib.paginator.Paginator

import lila.ui.*
import ScalatagsTemplate.{ *, given }
import lila.core.config.NetDomain

final class VideoUi(helpers: Helpers)(using NetDomain):
  import helpers.{ *, given }

  def show(video: Video, similar: Seq[VideoView], control: UserControl)(using PageContext) =
    div(cls := "show")(
      div(cls := "embed")(
        iframe(
          id  := "ytplayer",
          tpe := "text/html",
          src := s"https://www.youtube.com/embed/${video.id}?autoplay=1&origin=https://lichess.org&start=${video.startTime}",
          st.frameborder := "0",
          frame.allowfullscreen,
          frame.credentialless
        )
      ),
      h1(cls := "box__pad")(
        a(
          cls      := "is4 text",
          dataIcon := Icon.Back,
          href     := s"${routes.Video.index}?${control.queryString}"
        ),
        video.title
      ),
      div(cls := "meta box__pad")(
        div(cls := "target")(video.targets.map(lila.video.Target.name).mkString(", ")),
        a(cls := "author", href := s"${routes.Video.author(video.author)}?${control.queryString}")(
          video.author
        ),
        video.tags.map: tag =>
          a(
            cls      := "tag",
            dataIcon := Icon.Tag,
            href     := s"${routes.Video.index}?tags=${tag.replace(" ", "+")}"
          )(
            tag.capitalize
          ),
        video.metadata.description.map: desc =>
          p(cls := "description")(richText(desc))
      ),
      div(cls := "similar list box__pad")(
        similar.map { card(_, control) }
      )
    )

  def index(videos: Paginator[VideoView], count: Long, control: UserControl)(using ctx: PageContext) =
    frag(
      boxTop(
        h1(
          if control.filter.tags.nonEmpty then frag(pluralize("video", videos.nbResults), " found")
          else "Chess videos"
        ),
        searchForm(control.query)
      ),
      control.filter.tags.isEmpty.option(
        p(cls := "explain box__pad")(
          "All videos are free for everyone.",
          br,
          "Click one or many tags on the left to filter.",
          br,
          "We have carefully selected ",
          strong(count),
          " videos so far!"
        )
      ),
      div(cls := "list box__pad infinite-scroll")(
        videos.currentPageResults.map { card(_, control) },
        (videos.currentPageResults.sizeIs < 4).option(
          div(cls := s"not_much nb_${videos.nbResults}")(
            if videos.currentPageResults.isEmpty then "No videos for these tags:"
            else "That's all we got for these tags:",
            control.filter.tags.map { tag =>
              a(cls := "tag", dataIcon := Icon.Tag, href := s"${routes.Video.index}?tags=$tag")(
                tag.capitalize
              )
            },
            br,
            br,
            a(href := routes.Video.index, cls := "button")("Clear search")
          )
        ),
        pagerNext(videos, np => s"${routes.Video.index}?${control.queryString}&page=$np")
      )
    )

  def menu(control: UserControl)(using Context) =
    st.aside(cls := "page-menu__menu")(
      lila.ui.bits.subnav(
        control.tags.map: t =>
          val checked = control.filter.tags contains t.tag
          a(
            cls := List(
              "checked" -> checked,
              "full"    -> (checked || t.nb > 0),
              "empty"   -> !(checked || t.nb > 0)
            ),
            href := (checked || t.nb > 0)
              .option(s"${routes.Video.index}?${control.toggleTag(t.tag).queryString}")
          )(
            span(t.tag.capitalize),
            (!checked && t.nb > 0).option(em(t.nb))
          )
      ),
      div(cls := "under-tags")(
        if control.filter.tags.nonEmpty then
          a(cls := "button button-empty", href := routes.Video.index)("Clear search")
        else a(dataIcon := Icon.Tag, href := routes.Video.tags)("View more tags")
      )
    )

  def card(vv: VideoView, control: UserControl) =
    a(cls := "card paginated", href := s"${routes.Video.show(vv.video.id)}?${control.queryStringUnlessBot}")(
      vv.view.option(span(cls := "view")("watched")),
      span(cls := "duration")(vv.video.durationString),
      span(cls := "img", style := s"background-image: url(${vv.video.thumbnail})"),
      span(cls := "info")(
        span(cls := "title")(vv.video.title)
      ),
      span(cls := "reveal")(
        span(cls := "full-title")(vv.video.title),
        span(cls := "author")(vv.video.author),
        span(cls := "target")(vv.video.targets.map(Target.name).mkString(", "))
      )
    )

  def author(name: String, videos: Paginator[VideoView], control: UserControl)(using PageContext) =
    frag(
      boxTop(
        h1(
          a(
            cls      := "is4 text",
            dataIcon := Icon.Back,
            href     := s"${routes.Video.index}?${control.queryString}"
          ),
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

  def notFound(control: UserControl)(using PageContext) =
    boxTop(
      h1(
        a(
          cls      := "is4 text",
          dataIcon := Icon.Back,
          href     := s"${routes.Video.index}"
        ),
        "Video Not Found!"
      )
    )

  def searchForm(query: Option[String])(using PageContext) =
    form(cls := "search", method := "GET", action := routes.Video.index)(
      input(placeholder := trans.search.search.txt(), tpe := "text", name := "q", value := query)
    )

  def tags(ts: List[TagNb], control: UserControl)(using PageContext) =
    frag(
      boxTop(
        h1(
          a(cls := "text", dataIcon := Icon.Back, href := s"${routes.Video.index}?${control.queryString}")(
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

  def search(videos: Paginator[lila.video.VideoView], control: lila.video.UserControl)(using PageContext) =
    frag(
      boxTop(
        h1(pluralize("video", videos.nbResults), " found"),
        searchForm(control.query)
      ),
      div(cls := "list infinitescroll box__pad")(
        videos.currentPageResults.map { card(_, control) },
        (videos.currentPageResults.sizeIs < 4).option(
          div(cls := s"not_much nb_${videos.nbResults}")(
            if videos.currentPageResults.isEmpty then "No videos for this search:"
            else "That's all we got for this search:",
            s""""${~control.query}"""",
            br,
            br,
            a(href := routes.Video.index, cls := "button")("Clear search")
          )
        ),
        pagerNext(videos, np => s"${routes.Video.index}?${control.queryString}&page=$np")
      )
    )
