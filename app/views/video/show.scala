package views.html.video

import lila.common.String.html.richText

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object show {

  def apply(
      video: lila.video.Video,
      similar: Seq[lila.video.VideoView],
      control: lila.video.UserControl
  )(implicit ctx: Context) =
    layout(
      title = s"${video.title} • Free Chess Videos",
      control = control,
      openGraph = lila.app.ui
        .OpenGraph(
          title = s"${video.title} by ${video.author}",
          description = shorten(~video.metadata.description, 152),
          url = s"$netBaseUrl${routes.Video.show(video.id)}",
          `type` = "video",
          more = video.tags.map("video:tag" -> _)
        )
        .some
    ) {
      div(cls := "show")(
        div(cls := "embed")(
          iframe(
            id := "ytplayer",
            tpe := "text/html",
            src := s"https://www.youtube.com/embed/${video.id}?autoplay=1&origin=https://lichess.org&start=${video.startTime}",
            st.frameborder := "0",
            frame.allowfullscreen
          )
        ),
        h1(cls := "box__pad")(
          a(cls := "is4 text", dataIcon := "", href := s"${routes.Video.index}?${control.queryString}"),
          video.title
        ),
        div(cls := "meta box__pad")(
          div(cls := "target")(video.targets.map(lila.video.Target.name).mkString(", ")),
          a(cls := "author", href := s"${routes.Video.author(video.author)}?${control.queryString}")(
            video.author
          ),
          video.tags.map { tag =>
            a(
              cls := "tag",
              dataIcon := "",
              href := s"${routes.Video.index}?tags=${tag.replace(" ", "+")}"
            )(
              tag.capitalize
            )
          },
          video.metadata.description.map { desc =>
            p(cls := "description")(richText(desc))
          }
        ),
        div(cls := "similar list box__pad")(
          similar.map { bits.card(_, control) }
        )
      )
    }
}
