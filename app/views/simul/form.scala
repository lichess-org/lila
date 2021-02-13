package views.html.simul

import controllers.routes
import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.hub.LeaderTeam
import lila.simul.Simul
import lila.simul.SimulForm

object form {

  def create(form: Form[SimulForm.Setup], teams: List[LeaderTeam])(implicit
      ctx: Context
  ) =
    views.html.base.layout(
      title = trans.hostANewSimul.txt(),
      moreCss = cssTag("simul.form")
    ) {
      main(cls := "box box-pad page-small simul-form")(
        h1(trans.hostANewSimul()),
        postForm(cls := "form3", action := routes.Simul.create())(
          br,
          p(trans.whenCreateSimul()),
          br,
          br,
          formContent(form, teams, none),
          form3.actions(
            a(href := routes.Simul.home())(trans.cancel()),
            form3.submit(trans.hostANewSimul(), icon = "g".some)
          )
        )
      )
    }

  def edit(form: Form[SimulForm.Setup], teams: List[LeaderTeam], simul: Simul)(implicit
      ctx: Context
  ) =
    views.html.base.layout(
      title = s"Edit ${simul.fullName}",
      moreCss = cssTag("simul.form")
    ) {
      main(cls := "box box-pad page-small simul-form")(
        h1(s"Edit ${simul.fullName}"),
        postForm(cls := "form3", action := routes.Simul.update(simul.id))(
          formContent(form, teams, simul.some),
          form3.actions(
            a(href := routes.Simul.show(simul.id))(trans.cancel()),
            form3.submit(trans.save(), icon = "g".some)
          )
        )
      )
    }

  private def formContent(form: Form[SimulForm.Setup], teams: List[LeaderTeam], simul: Option[Simul])(implicit
      ctx: Context
  ) = {
    import lila.simul.SimulForm._
    frag(
      globalError(form),
      form3.group(form("name"), trans.name()) { f =>
        div(
          form3.input(f),
          " Simul",
          br,
          small(cls := "form-help")(trans.inappropriateNameWarning())
        )
      },
      form3.group(form("variant"), trans.simulVariantsHint()) { f =>
        frag(
          div(cls := "variants")(
            views.html.setup.filter.renderCheckboxes(
              form,
              "variants",
              translatedVariantChoicesWithVariants,
              checks = form.value.??(_.variants.map(_.toString).toSet)
            )
          ),
          errMsg(f)
        )
      },
      form3.split(
        form3.group(
          form("clockTime"),
          raw("Clock initial time"),
          help = trans.simulClockHint().some,
          half = true
        )(form3.select(_, clockTimeChoices)),
        form3.group(form("clockIncrement"), raw("Clock increment"), half = true)(
          form3.select(_, clockIncrementChoices)
        )
      ),
      form3.split(
        form3.group(
          form("clockExtra"),
          trans.simulHostExtraTime(),
          help = trans.simulAddExtraTime().some,
          half = true
        )(
          form3.select(_, clockExtraChoices)
        ),
        form3.group(form("color"), raw("Host color for each game"), half = true)(
          form3.select(_, colorChoices)
        )
      ),
      form3.split(
        teams.nonEmpty ?? {
          form3.group(form("team"), raw("Only members of team"), half = true)(
            form3.select(_, List(("", "No Restriction")) ::: teams.map(_.pair))
          )
        },
        form3.group(
          form("position"),
          trans.startPosition(),
          klass = "position",
          half = true,
          help = frag("Custom starting position only works with the standard variant.").some
        ) { field =>
          st.select(
            id := form3.id(field),
            st.name := field.name,
            cls := "form-control"
          )(
            option(
              value := chess.StartingPosition.initial.fen,
              field.value.has(chess.StartingPosition.initial.fen) option selected
            )(chess.StartingPosition.initial.name),
            chess.StartingPosition.categories.map { categ =>
              optgroup(attr("label") := categ.name)(
                categ.positions.map { v =>
                  option(value := v.fen, field.value.has(v.fen) option selected)(v.fullName)
                }
              )
            }
          )
        }
      ),
      form3.group(
        form("text"),
        raw("Simul description"),
        help = frag("Anything you want to tell the participants?").some
      )(form3.textarea(_)(rows := 10)),
      ctx.me.exists(_.hasTitle) option form3.checkbox(
        form("featured"),
        frag("Feature on lichess.org/simul"),
        help = frag("Show your simul to everyone on lichess.org/simul. Disable for private simuls.").some
      )
    )
  }
}
