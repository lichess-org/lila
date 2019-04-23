package views.html.setup

import play.api.data.Form
import play.api.mvc.Call

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
    trans.createAGame(),
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
            div(cls := "rating-range-config slider")(
              trans.ratingRange(),
              ": ",
              span(cls := "range")("? - ?"),
              div(cls := "rating-range")(
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
    layout(form, "ai", trans.playWithTheMachine(), routes.Setup.ai) {
      frag(
        renderVariant(form, translatedAiVariantChoices),
        fenInput(form("fen"), true, true, validFen),
        renderTimeMode(form, lidraughts.setup.AiConfig),
        if (ctx.blind) frag(
          renderLabel(form("level"), trans.level()),
          renderSelect(form("level"), lidraughts.setup.AiConfig.levelChoices),
          blindSideChoice(form)
        )
        else frag(
          br,
          trans.level(),
          div(cls := "level buttons")(
            div(id := "config_level")(
              renderRadios(form("level"), lidraughts.setup.AiConfig.levelChoices)
            ),
            div(cls := "ai_info")(
              ratings.toList.map {
                case (level, rating) => div(cls := s"${prefix}level_$level")(trans.aiNameLevelAiLevel("A.I.", level))
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
      error.map(e => raw(e.replace("{{user}}", userIdLink(user.map(_.id)).toString)))
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
      renderLabel(form("color"), trans.side()),
      renderSelect(form("color").copy(value = "random".some), translatedSideChoices)
    )

  private def layout(
    form: Form[_],
    typ: String,
    titleF: Frag,
    route: Call,
    error: Option[Frag] = None
  )(fields: Frag)(implicit ctx: Context) =
    div(cls := error.isDefined option "error")(
      h2(titleF),
      error.map { e =>
        frag(
          p(cls := "error")(e),
          br,
          a(href := routes.Lobby.home, cls := "button text", dataIcon := "L")(trans.cancel.txt())
        )
      }.getOrElse {
        st.form(action := route, method := "post", novalidate,
          dataRandomColorVariants,
          dataType := typ,
          dataAnon := ctx.isAnon.option("1"))(
            fields,
            if (ctx.blind) button(tpe := "submit")("Create the game")
            else div(cls := "color-submits")(
              translatedSideChoices.map {
                case (key, name, _) => button(
                  (typ == "hook") option disabled,
                  tpe := "submit",
                  title := name,
                  cls := s"color-submits__button button button-green $key",
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
              trans.perfRatingX(
                raw(s"""<strong data-icon="${perfType.iconChar}">${me.perfs(perfType.key).map(_.intRating).getOrElse("?")}</strong> ${perfType.name}""")
              )
            )
          }
        )
      }
    )
}
