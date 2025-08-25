package lila.video
package ui

import scalalib.paginator.Paginator

import lila.core.config.NetDomain
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class VideoUi(helpers: Helpers)(using NetDomain):
  import helpers.{ *, given }
  import trans.video as trv

  private def page(title: String, control: UserControl)(using ctx: Context) =
    Page(title)
      .css("bits.video")
      .i18n(_.video)
      .js(infiniteScrollEsmInit)
      .flag(_.fullScreen)
      .wrap: body =>
        main(cls := "video page-menu force-ltr")(
          menu(control),
          div(cls := "page-menu__content box")(body)
        )

  def show(video: Video, similar: Seq[VideoView], control: UserControl)(using ctx: Context) =
    page(s"${video.title} • ${trv.freeChessVideos}", control)
      .graph(
        OpenGraph(
          title = s"${video.title} ${trv.by} ${video.author}",
          description = shorten(~video.metadata.description, 152),
          url = s"$netBaseUrl${routes.Video.show(video.id)}",
          `type` = "video"
        )
      ):
        div(cls := "show")(
          div(cls := "embed")(
            iframe(
              id := "ytplayer",
              tpe := "text/html",
              src := s"https://www.youtube.com/embed/${video.id}?autoplay=1&origin=https://lichess.org&start=${video.startTime}",
              st.frameborder := "0",
              frame.allowfullscreen,
              frame.credentialless
            )
          ),
          h1(cls := "box__pad")(
            a(
              cls := "is4 text",
              dataIcon := Icon.Back,
              href := s"${routes.Video.index}?${control.queryString}"
            ),
            video.title
          ),
          div(cls := "meta box__pad")(
            div(cls := "target")(video.targets.map(Target.name).mkString(", ")),
            a(
              cls := "author",
              href := s"${routes.Video.author(video.author)}?${control.queryString}"
            )(video.author),
            video.tags.map: tag =>
              a(
                cls := "tag",
                dataIcon := Icon.Tag,
                href := s"${routes.Video.index}?tags=${tag.replace(" ", "+")}"
              )(tag.capitalize),
            video.metadata.description.map: desc =>
              p(cls := "description")(richText(desc))
          ),
          div(cls := "similar list box__pad"):
            similar.map(card(_, control))
        )

  def index(videos: Paginator[VideoView], count: Long, control: UserControl)(using ctx: Context) =
    val tagString = control.filter.tags.some.filter(_.nonEmpty).so(_.mkString(" + ") + " • ")
    page(s"${tagString}${trv.freeChessVideos}", control)
    .hrefLangs(lila.ui.LangPath(routes.Video.index))
      .graph(
        title = s"${tagString}${trv.freeCarefullyCurated}",
        description = s"${videos.nbResults} ${trv.curatedChessVideos}${
            if tagString.nonEmpty then s" ${trv.matchingTheTags} " + tagString
            else " • "
          }${trv.freeForAll}",
        url = s"$netBaseUrl${routes.Video.index}?${control.queryString}"
      ):
        frag(
          boxTop(
            h1(
              if control.filter.tags.nonEmpty then frag(pluralize("video", videos.nbResults), s" ${trv.found}")
              else trv.chessVideos
            ),
            searchForm(control.query)
          ),
          control.filter.tags.isEmpty.option(
            p(cls := "explain box__pad")(
              trv.allVideosAreFree(),
              br,
              trv.clickOneOrMany(),
              br,
              trv.weHaveCarefullySelected(count.toString()),
            )
          ),
          div(cls := "list box__pad infinite-scroll")(
            videos.currentPageResults.map { card(_, control) },
            (videos.currentPageResults.sizeIs < 4).option(
              div(cls := s"not_much nb_${videos.nbResults}")(
                if videos.currentPageResults.isEmpty then trv.noVideosForTheseTags
                else trv.thatsAllWeGotForTheseTags,
                control.filter.tags.map { tag =>
                  a(cls := "tag", dataIcon := Icon.Tag, href := s"${routes.Video.index}?tags=$tag")(
                    tag.capitalize
                  )
                },
                br,
                br,
                a(href := routes.Video.index, cls := "button")(trv.clearSearch)
              )
            ),
            pagerNext(videos, np => s"${routes.Video.index}?${control.queryString}&page=$np")
          )
        )

  private def menu(control: UserControl)/*(using ctx: Context)*/ =
    st.aside(cls := "page-menu__menu")(
      lila.ui.bits.subnav(
        control.tags.map: t =>
          val checked = control.filter.tags contains t.tag
          a(
            cls := List(
              "checked" -> checked,
              "full" -> (checked || t.nb > 0),
              "empty" -> !(checked || t.nb > 0)
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
          a(cls := "button button-empty", href := routes.Video.index)(trv.clearSearch)
        else a(dataIcon := Icon.Tag, href := routes.Video.tags)(trv.viewMoreTags)
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

  def author(name: String, videos: Paginator[VideoView], control: UserControl)(using ctx: Context) =
    page(s"$name • ${trv.freeChessVideos}", control):
      frag(
        boxTop(
          h1(
            a(
              cls := "is4 text",
              dataIcon := Icon.Back,
              href := s"${routes.Video.index}?${control.queryString}"
            ),
            name
          ),
          span(
            pluralize("video", videos.nbResults),
            s" ${trv.found}"
          )
        ),
        div(cls := "list infinite-scroll box__pad")(
          videos.currentPageResults.map { card(_, control) },
          pagerNext(videos, np => s"${routes.Video.author(name)}?${control.queryString}&page=$np")
        )
      )

  def notFound(control: UserControl)(using ctx: Context) =
    page(trv.videoNotFound, control):
      boxTop(
        h1(
          a(
            cls := "is4 text",
            dataIcon := Icon.Back,
            href := s"${routes.Video.index}"
          ),
          s"${trv.videoNotFound}!"
        )
      )

  def searchForm(query: Option[String])(using Context) =
    form(cls := "search", method := "GET", action := routes.Video.index):
      input(placeholder := trans.search.search.txt(), tpe := "text", name := "q", value := query)

  def tags(ts: List[TagNb], control: UserControl)(using ctx: Context) =
    page(s"Tags • ${trv.freeChessVideos}", control):
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
              em(" " + t.nb)
            )
          }
        )
      )

  def search(videos: Paginator[VideoView], control: UserControl)(using Context) =
    page(s"${control.query.getOrElse("Search")} • ${trv.freeChessVideos}", control):
      frag(
        boxTop(
          h1(pluralize("video", videos.nbResults), " found"),
          searchForm(control.query)
        ),
        div(cls := "list infinitescroll box__pad")(
          videos.currentPageResults.map { card(_, control) },
          (videos.currentPageResults.sizeIs < 4).option(
            div(cls := s"not_much nb_${videos.nbResults}")(
              if videos.currentPageResults.isEmpty then trv.noVideosForThisSearch
              else trv.thatsAllWeGotForThisSearch,
              s""""${~control.query}"""",
              br,
              br,
              a(href := routes.Video.index, cls := "button")(trv.clearSearch)
            )
          ),
          pagerNext(videos, np => s"${routes.Video.index}?${control.queryString}&page=$np")
        )
      )
