package views.html.setup

import play.api.mvc.Call
import play.api.data.Form
import play.twirl.api.Html

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object config {

  private val dataRandomColorVariants =
    attr("data-random-color-variants") := lila.game.Game.variantsWhereWhiteIsBetter.map(_.id).mkString(",")

  private val dataAnon = attr("data-anon")

  def apply(
    form: Form[_],
    typ: String,
    title: Html,
    route: Call,
    fields: Html,
    error: Option[Html] = None
  )(implicit ctx: Context) =
    div(
      cls := s"""lichess_overboard game_config game_config_$typ${error.isDefined ?? " error"}""",
      dataRandomColorVariants,
      dataAnon := ctx.isAnon.option("1")
    )(
        a(href := routes.Lobby.home, cls := "close icon", st.title := trans.cancel.txt(), dataIcon := "L"),
        h2(title),
        error.map { e =>
          frag(
            p(cls := "error")(e),
            br,
            a(href := routes.Lobby.home, cls := "button text", dataIcon := "L")(trans.cancel.txt())
          )
        }.getOrElse {
          st.form(action := route, novalidate := true)(
            fields,
            div(cls := "color_submits")(
              List(
                "black" -> trans.black(),
                "random" -> trans.randomColor(),
                "white" -> trans.white()
              ).map { color =>
                  button(
                    disabled := typ == "hook",
                    dataHint := "submit",
                    cls := s"button hint--bottom ${color._1}",
                    name := form("color").id,
                    value := color._1
                  )(i)
                }
            )
          )
        },
        ctx.me.map { me =>
          div(cls := "ratings")(
            lila.rating.PerfType.nonPuzzle.map { perfType =>
              div(cls := perfType.key)(
                trans.perfRatingX.frag(
                  Html(s"""<strong data-icon="${perfType.iconChar}">${me.perfs(perfType.key).map(_.intRating).getOrElse("?")}</strong> ${perfType.name}""")
                )
              )
            }
          )
        }
      )
}
