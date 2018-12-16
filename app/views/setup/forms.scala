package views.html.setup

import play.api.data.Form
import play.api.mvc.Call
import play.twirl.api.Html

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.rating.RatingRange
import lila.setup.{ FriendConfig, HookConfig }
import lila.user.User

import controllers.routes

object forms {

  import bits._

  def hook(form: Form[_])(implicit ctx: Context) = layout(
    form,
    "hook",
    trans.createAGame.frag(),
    routes.Setup.hook("uid-placeholder")
  ) {
      frag(
        renderVariant(form, translatedVariantChoicesWithVariants),
        renderTimeMode(form, lila.setup.HookConfig),
        ctx.isAuth option frag(
          div(cls := "mode_choice buttons")(
            renderRadios(form("mode"), translatedModeChoices)
          ),
          div(cls := "optional_config")(
            div(cls := "rating_range_config slider")(
              trans.ratingRange(),
              ": ",
              span(cls := "range")("? - ?"),
              div(cls := "rating_range")(
                renderInput(form("ratingRange"))(
                  `type` := "hidden",
                  dataMin := RatingRange.min,
                  dataMax := RatingRange.max
                )
              )
            )
          )
        )
      )
    }

  def ai(form: Form[_], ratings: Map[Int, Int], validFen: Option[lila.setup.ValidFen])(implicit ctx: Context) =
    layout(form, "ai", trans.playWithTheMachine(), routes.Setup.ai) {
      frag(
        renderVariant(form, translatedAiVariantChoices),
        fenInput(form("fen"), true, validFen),
        renderTimeMode(form, lila.setup.AiConfig),
        trans.level(),
        div(cls := "level buttons")(
          div(id := "config_level")(
            renderRadios(form("level"), lila.setup.AiConfig.levelChoices)
          ),
          div(cls := "ai_info")(
            ratings.toList.map {
              case (level, rating) => div(cls := s"level_$level")(trans.aiNameLevelAiLevel("A.I.", level))
            }
          )
        )
      )
    }

  def friend(
    form: Form[_],
    user: Option[User],
    error: Option[String],
    validFen: Option[lila.setup.ValidFen]
  )(implicit ctx: Context) =
    layout(
      form,
      "friend",
      (if (user.isDefined) trans.challengeToPlay else trans.playWithAFriend)(),
      routes.Setup.friend(user map (_.id)),
      error.map(e => Html(e.replace("{{user}}", userIdLink(user.map(_.id)).toString)))
    )(frag(
        user.map { u =>
          userLink(u, cssClass = "target".some)
        },
        renderVariant(form, translatedVariantChoicesWithVariantsAndFen),
        fenInput(form("fen"), false, validFen),
        renderTimeMode(form, lila.setup.FriendConfig),
        ctx.isAuth option div(cls := "mode_choice buttons")(
          renderRadios(form("mode"), translatedModeChoices)
        )
      ))

  private def layout(
    form: Form[_],
    typ: String,
    title: Frag,
    route: Call,
    error: Option[Frag] = None
  )(fields: Frag)(implicit ctx: Context) =
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
          st.form(action := route, method := "post", novalidate := true)(
            fields,
            div(cls := "color_submits")(
              List(
                "black" -> trans.black.txt(),
                "random" -> trans.randomColor.txt(),
                "white" -> trans.white.txt()
              ).map {
                  case (key, name) => button(
                    disabled := typ == "hook" option true,
                    `type` := "submit",
                    dataHint := name,
                    cls := s"button hint--bottom $key",
                    st.name := form("color").id,
                    value := key
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
