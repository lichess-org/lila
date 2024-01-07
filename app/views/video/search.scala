package views.html.video

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.paginator.Paginator

import controllers.routes

object search:

  import trans.video.*

  def apply(videos: Paginator[lila.video.VideoView], control: lila.video.UserControl)(using PageContext) =
    layout(title = s"${control.query.getOrElse("Search")} • Free Chess Videos", control = control)(
      boxTop(
        h1(nbVideosFound(videos.nbResults)),
        bits.searchForm(control.query)
      ),
      div(cls := "list infinitescroll box__pad")(
        videos.currentPageResults.map { bits.card(_, control) },
        videos.currentPageResults.sizeIs < 4 option div(cls := s"not_much nb_${videos.nbResults}")(
          if videos.currentPageResults.isEmpty then noVideosForThisSearch()
          else thatIsAllWeGotForThisSearch(),
          s""""${~control.query}"""",
          br,
          br,
          a(href := routes.Video.index, cls := "button")(clearSearch())
        ),
        pagerNext(videos, np => s"${routes.Video.index}?${control.queryString}&page=$np")
      )
    )
