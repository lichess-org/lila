package views.html

import controllers.routes
import play.api.libs.json.Json

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.pref.Pref.Color

object coordinate {

  def home(scoreOption: Option[lila.coordinate.Score])(implicit ctx: Context) =
    views.html.base.layout(
      title = trans.coordinates.coordinateTraining.txt(),
      moreCss = cssTag("misc.coordinate"),
      moreJs = frag(
        chartTag,
        jsTag("chart.coordinate"),
        moduleJsTag(
          "misc.coordinate",
          Json.obj(
            "colorPref" -> ctx.pref.coordColorName,
            "scoreUrl"  -> ctx.isAuth.option(routes.Coordinate.score.url),
            "points" -> scoreOption.map(s => {
              Json.obj(
                "sente" -> s.sente,
                "gote"  -> s.gote,
              )
            }),
          ),
        ),
      ),
      openGraph = lila.app.ui
        .OpenGraph(
          title = trans.coordinates.coordinateTraining.txt(),
          url = s"$netBaseUrl${routes.Coordinate.home.url}",
          description = trans.coordinates.aSquareNameAppears.txt(),
        )
        .some,
      zoomable = true,
      withHrefLangs = lila.i18n.LangList.All.some,
    )(
      main(
        id  := "trainer",
        cls := "coord-trainer training init",
      )(
        div(cls := "coord-trainer__side")(
          div(cls := "box")(
            h1(trans.coordinates.coordinates()),
            if (ctx.isAuth) scoreOption.map { score =>
              div(cls := "scores")(scoreCharts(score))
            },
          ),
          form(cls := "color buttons", action := routes.Coordinate.color, method := "post")(
            st.group(cls := "radio")(
              List(Color.SENTE, Color.RANDOM, Color.GOTE).map { id =>
                val colorName = Color.name(id)
                val translated =
                  shogi.Color.fromName(colorName).fold(trans.randomColor.txt())(standardColorName)
                div(
                  input(
                    tpe   := "radio",
                    st.id := s"coord_color_$id",
                    name  := "color",
                    value := id,
                    (id == ctx.pref.coordColor) option checked,
                  ),
                  label(`for` := s"coord_color_$id", cls := s"color", title := translated)(
                    div(cls := s"color-icon ${colorName}"),
                  ),
                )
              },
            ),
          ),
        ),
        div(cls := "coord-trainer__board main-board")(
          div(cls := "next_coord", id := "next_coord0"),
          div(cls := "next_coord", id := "next_coord1"),
          shogigroundEmpty(
            shogi.variant.Standard,
            shogi.Color.fromSente(ctx.pref.coordColor != Color.GOTE),
          ),
        ),
        div(cls := "coord-trainer__table")(
          div(cls := "explanation")(
            p(trans.coordinates.knowingTheShogiBoard()),
            ul(
              li(trans.coordinates.mostShogiCourses()),
              li(trans.coordinates.talkToYourShogiFriends()),
              li(trans.coordinates.youCanAnalyseAGameMoreEffectively()),
            ),
            p(trans.coordinates.aSquareNameAppears()),
          ),
          div(cls := "box current-status")(
            h2(trans.storm.score()),
            div(cls := "coord-trainer__score")(0),
          ),
          div(cls := "box current-status")(
            h2(trans.time()),
            div(cls := "coord-trainer__timer")(30.0),
          ),
        ),
        div(cls := "coord-trainer__button")(
          div(cls := "coord-start")(
            button(cls := "start button button-fat")(trans.coordinates.startTraining()),
          ),
          div(cls := "current-color"),
        ),
        div(cls := "coord-trainer__progress")(div(cls := "progress_bar")),
      ),
    )

  def scoreCharts(score: lila.coordinate.Score)(implicit ctx: Context) =
    frag(
      List(
        (shogi.Color.Sente, score.sente),
        (shogi.Color.Gote, score.gote),
      ).map { case (c, s) =>
        div(cls := "chart_container")(
          s.nonEmpty option frag(
            p(
              trans.coordinates.averageScoreAsXY(
                standardColorName(c).toUpperCase,
                raw(s"""<strong>${"%.2f".format(s.sum.toDouble / s.size)}</strong>"""),
              ),
            ),
            canvas(cls := "user_chart"),
          ),
        )
      },
    )
}
