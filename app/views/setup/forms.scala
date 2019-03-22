package views.html.setup

import play.api.data.Form
import play.api.mvc.Call
import play.twirl.api.Html

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.rating.RatingRange
import lidraughts.setup.{ FriendConfig, HookConfig }
import lidraughts.user.User

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
        renderTimeMode(form, lidraughts.setup.HookConfig),
        ctx.isAuth option frag(
          div(cls := "mode_choice buttons")(
            renderRadios(form("mode"), translatedModeChoices)
          ),
          ctx.noBlind option div(cls := "optional_config")(
            div(cls := "rating_range_config slider")(
              trans.ratingRange.frag(),
              ": ",
              span(cls := "range")("? - ?"),
              div(cls := "rating_range")(
                renderInput(form("ratingRange"))(
                  dataMin := RatingRange.min,
                  dataMax := RatingRange.max
                )
              )
            )
          )
        )
      )
    }

  def ai(form: Form[_], ratings: Map[Int, Int], validFen: Option[lidraughts.setup.ValidFen])(implicit ctx: Context) =
    layout(form, "ai", trans.playWithTheMachine.frag(), routes.Setup.ai) {
      frag(
        renderVariant(form, translatedAiVariantChoices),
        fenInput(form("fen"), true, true, validFen),
        renderTimeMode(form, lidraughts.setup.AiConfig),
        if (ctx.blind) frag(
          renderLabel(form("level"), trans.level.frag()),
          renderSelect(form("level"), lidraughts.setup.AiConfig.levelChoices),
          blindSideChoice(form)
        )
        else frag(
          trans.level.frag(),
          div(cls := "level buttons")(
            div(id := "config_level")(
              renderRadios(form("level"), lidraughts.setup.AiConfig.levelChoices)
            ),
            div(cls := "ai_info")(
              ratings.toList.map {
                case (level, rating) => div(cls := s"${prefix}level_$level")(trans.aiNameLevelAiLevel.frag("A.I.", level))
              }
            )
          )
        )
      )
    }

  def friend(
    form: Form[_],
    user: Option[User],
    error: Option[String],
    validFen: Option[lidraughts.setup.ValidFen]
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
        fenInput(form("fen"), false, false, validFen),
        renderTimeMode(form, lidraughts.setup.FriendConfig),
        ctx.isAuth option div(cls := "mode_choice buttons")(
          renderRadios(form("mode"), translatedModeChoices)
        ),
        blindSideChoice(form)
      ))

  private def blindSideChoice(form: Form[_])(implicit ctx: Context) =
    ctx.blind option frag(
      renderLabel(form("color"), trans.side.frag()),
      renderSelect(form("color").copy(value = "random".some), translatedSideChoices)
    )

  private def layout(
    form: Form[_],
    typ: String,
    title: Frag,
    route: Call,
    error: Option[Frag] = None
  )(fields: Frag)(implicit ctx: Context) =
    div(
      cls := s"""lidraughts_overboard game_config game_config_$typ${error.isDefined ?? " error"}""",
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
            if (ctx.blind) button(tpe := "submit")("Create the game")
            else div(cls := "color_submits")(
              translatedSideChoices.map {
                case (key, name, _) => button(
                  disabled := typ == "hook" option true,
                  tpe := "submit",
                  dataHint := ctx.noBlind option name,
                  cls := s"button hint--bottom $key",
                  st.name := "color",
                  value := key
                )(i)
              }
            )
          )
        },
        ctx.me.ifFalse(ctx.blind).map { me =>
          div(cls := "ratings")(
            lidraughts.rating.PerfType.nonPuzzle.map { perfType =>
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
