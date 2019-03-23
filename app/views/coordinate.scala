package views.html

import play.api.libs.json.Json
import play.twirl.api.Html

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue
import lila.pref.Pref.Color

import controllers.routes

object coordinate {

  def home(scoreOption: Option[lila.coordinate.Score])(implicit ctx: Context) =
    views.html.base.layout(
      title = trans.coordinates.coordinateTraining.txt(),
      responsive = true,
      moreCss = responsiveCssTag("coordinate"),
      moreJs = frag(
        jsTag("vendor/sparkline.min.js"),
        jsAt("compiled/coordinate.js")
      ),
      openGraph = lila.app.ui.OpenGraph(
        title = "Chess board coordinates trainer",
        url = s"$netBaseUrl${routes.Coordinate.home.url}",
        description = "Knowing the chessboard coordinates is a very important chess skill. A square name appears on the board and you must click on the correct square."
      ).some,
      zoomable = true
    )(
        main(
          id := "trainer",
          cls := "training init",
          attr("data-color-pref") := ctx.pref.coordColorName,
          attr("data-score-url") := ctx.isAuth.option(routes.Coordinate.score().url)
        )(
            div(cls := "overlay_container")(
              div(cls := "next_coord", id := "next_coord0"),
              div(cls := "next_coord", id := "next_coord1"),
              div(cls := "next_coord", id := "next_coord2"),
              div(cls := "score_container")(strong(cls := "score")(0))
            ),
            div(cls := "side")(
              div(cls := "box")(
                h1(trans.coordinates.coordinates.frag()),
                if (ctx.isAuth) scoreOption.map { score =>
                  div(cls := "scores")(scoreCharts(score))
                }
                else div(cls := "register")(
                  p(trans.toTrackYourProgress.frag()),
                  p(cls := "signup")(
                    a(cls := "button", href := routes.Auth.signup)(trans.signUp.frag())
                  )
                )
              ),
              form(cls := "color buttons", action := routes.Coordinate.color)(
                st.group(cls := "radio")(
                  List(Color.BLACK, Color.RANDOM, Color.WHITE).map { id =>
                    div(
                      input(
                        tpe := "radio",
                        st.id := s"coord_color_$id",
                        name := "coord_color",
                        value := id,
                        checked := (id == ctx.pref.coordColor).option(true)
                      ),
                      label(`for` := s"coord_color_$id", cls := s"color color_$id")(i)
                    )
                  }
                )
              )
            ),
            div(cls := "board_and_ground")(
              div(cls := "boards")(div(cls := "cg-board-wrap")),
              div(cls := "right")(
                div(cls := "explanation")(
                  p(trans.coordinates.knowingTheChessBoard.frag()),
                  ul(
                    li(trans.coordinates.mostChessCourses.frag()),
                    li(trans.coordinates.talkToYourChessFriends.frag()),
                    li(trans.coordinates.youCanAnalyseAGameMoreEffectively.frag())
                  ),
                  p(trans.coordinates.aSquareNameAppears.frag())
                ),
                button(cls := "start button")(trans.coordinates.startTraining.frag())
              )
            ),
            div(cls := "progress_bar_container")(div(cls := "progress_bar"))
          )
      )

  def scoreCharts(score: lila.coordinate.Score)(implicit ctx: Context) = frag(
    List((trans.coordinates.averageScoreAsWhiteX, score.white), (trans.coordinates.averageScoreAsBlackX, score.black)).map {
      case (averageScoreX, s) => div(cls := "chart_container")(
        s.nonEmpty option frag(
          p(averageScoreX(Html(s"""<strong>${"%.2f".format(s.sum.toDouble / s.size)}</strong>"""))),
          div(cls := "user_chart", attr("data-points") := safeJsonValue(Json toJson s))
        )
      )
    }
  )
}
