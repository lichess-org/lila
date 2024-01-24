package views.html.setup

import play.api.data.Form
import play.api.mvc.Call

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.rating.RatingRange
import lila.user.User

import controllers.routes

object forms {

  import bits._

  def hook(form: Form[_])(implicit ctx: Context) =
    layout(
      "hook",
      trans.createAGame(),
      routes.Setup.hook("sri-placeholder")
    ) {
      frag(
        renderVariant(form, translatedVariantChoices),
        renderTimeMode(form),
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

  def ai(form: Form[_], validSfen: Option[lila.setup.ValidSfen])(implicit
      ctx: Context
  ) =
    layout("ai", trans.playWithTheMachine(), routes.Setup.ai) {
      frag(
        renderVariant(form, translatedAiChoices),
        sfenInput(form, true, validSfen),
        renderTimeMode(form),
        if (ctx.blind)
          frag(
            renderLabel(form("level"), trans.strength()),
            renderSelect(form("level"), lila.setup.AiConfig.levelChoices),
            blindSideChoice(form)
          )
        else
          frag(
            br,
            trans.strength(),
            div(cls := "level buttons")(
              div(id := "config_level")(
                renderRadios(form("level"), lila.setup.AiConfig.levelChoices)
              ),
              div(cls := "ai_info")
            )
          )
      )
    }

  def friend(
      form: Form[_],
      user: Option[User],
      error: Option[String],
      validSfen: Option[lila.setup.ValidSfen]
  )(implicit ctx: Context) =
    layout(
      "friend",
      (if (user.isDefined) trans.challengeToPlay else trans.playWithAFriend) (),
      routes.Setup.friend(user map (_.id)),
      error.map(e => raw(e.replace("{{user}}", userIdLink(user.map(_.id)).toString)))
    )(
      frag(
        user.map { u =>
          userLink(u, cssClass = "target".some)
        },
        renderVariant(form, translatedVariantChoices),
        sfenInput(form, false, validSfen),
        renderTimeMode(form),
        ctx.isAuth option div(cls := "mode_choice buttons")(
          renderRadios(form("mode"), translatedModeChoices)
        ),
        blindSideChoice(form)
      )
    )

  private def translatedSideChoices(implicit ctx: Context) =
    List(
      ("sente", standardColorName(shogi.Color.Sente), none),
      ("random", trans.randomColor.txt(), none),
      ("gote", standardColorName(shogi.Color.Gote), none)
    )

  private def blindSideChoice(form: Form[_])(implicit ctx: Context) =
    ctx.blind option frag(
      renderLabel(form("color"), trans.side()),
      renderSelect(form("color").copy(value = "random".some), translatedSideChoices)
    )

  private def layout(
      typ: String,
      titleF: Frag,
      route: Call,
      error: Option[Frag] = None
  )(fields: Frag)(implicit ctx: Context) =
    div(cls := error.isDefined option "error")(
      h2(titleF),
      error
        .map { e =>
          frag(
            p(cls := "error")(e),
            br,
            a(href := routes.Lobby.home, cls := "button text", dataIcon := "L")(trans.cancel.txt())
          )
        }
        .getOrElse {
          postForm(
            action := route,
            novalidate,
            dataType := typ,
            dataAnon := ctx.isAnon.option("1")
          )(
            fields,
            if (ctx.blind) submitButton("Create the game")
            else
              div(cls := "color-submits")(
                translatedSideChoices.map {
                  case (key, name, _) => {
                    submitButton(
                      (typ == "hook") option disabled,
                      title   := name,
                      cls     := s"color-submits__button button button-metal $key",
                      st.name := "color",
                      value   := key
                    )(i)
                  }
                },
                div(cls := "submit-error-message")
              )
          )
        },
      ctx.me.ifFalse(ctx.blind).map { me =>
        div(cls := "ratings")(
          lila.rating.PerfType.nonPuzzle.map { perfType =>
            div(cls := perfType.key)(
              trans.perfRatingX(
                raw(s"""<strong data-icon="${perfType.iconChar}">${me
                    .perfs(perfType.key)
                    .map(_.intRating)
                    .getOrElse("?")}</strong> ${perfType.trans}""")
              )
            )
          }
        )
      }
    )
}
