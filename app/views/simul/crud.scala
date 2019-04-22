package views.html
package simul

import play.api.data.Form

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.common.paginator.Paginator
import lidraughts.simul.{ DataForm, Simul }
import lidraughts.simul.crud.CrudForm
import lidraughts.rating.PerfType

import controllers.routes

object crud {

  private def layout(title: String, evenMoreJs: Frag = emptyFrag, css: String = "mod.misc")(body: Frag)(implicit ctx: Context) =
    views.html.base.layout(
      title = title,
      moreCss = cssTag(css),
      moreJs = frag(
        flatpickrTag,
        delayFlatpickrStart,
        evenMoreJs
      )
    ) {
        main(cls := "page-menu")(
          views.html.mod.menu("simul"),
          body
        )
      }

  def create(form: Form[lidraughts.simul.crud.CrudForm.Data])(implicit ctx: Context) = layout(
    title = "New simul",
    css = "mod.form"
  ) {
    div(cls := "crud page-menu__content box box-pad")(
      h1("New simul"),
      st.form(cls := "content_box_content form3", action := routes.SimulCrud.create, method := "POST")(inForm(form, false))
    )
  }

  def edit(simul: Simul, form: Form[lidraughts.simul.crud.CrudForm.Data])(implicit ctx: Context) = layout(
    title = simul.fullName,
    css = "mod.form"
  ) {
    val limitedEdit = simul.isRunning || simul.isFinished
    div(cls := "crud edit page-menu__content box box-pad")(
      h1(
        a(href := routes.Simul.show(simul.id))(simul.fullName),
        " ",
        span("Created on ", showDate(simul.createdAt))
      ),
      st.form(cls := "content_box_content form3", action := routes.SimulCrud.update(simul.id), method := "POST")(inForm(form, limitedEdit)),
      allowedPlayers
    )
  }

  private def allowedPlayers(implicit ctx: Context) = frag(
    div(cls := "crud edit page-menu__content box box-pad")(
      h2("Optionally restrict the simul to the usernames listed below"),
      st.form(cls := "content_box_content form3", method := "POST")(
        form3.split(
          div(cls := "form-group form-half")(
            label(cls := "form-label", `for` := "player")("Add player"),
            input(
              cls := s"form-control user-autocomplete",
              required := "required",
              name := "player",
              id := "player",
              dataTag := "span"
            ),
            form3.action(button(cls := "submit button text", id := "submit_player", dataIcon := "E")("Allow player in simul"))
          ),
          div(cls := "players form-group form-half")(
            table(cls := "slist")(
              thead(
                tr(th(colspan := "2")("Allowed players (userid)"))
              ),
              tbody()
            )
          )
        )
      )
    ),
    jQueryTag,
    jsAt("javascripts/simul-allow.js")
  )

  private def inForm(form: Form[lidraughts.simul.crud.CrudForm.Data], limitedEdit: Boolean)(implicit ctx: Context) = {
    import DataForm._
    frag(
      form3.split(
        form3.group(form("date"), raw("Start date <strong>UTC</strong>"), half = true)(form3.flatpickr(_)),
        form3.group(form("name"), raw("Name"), help = raw("Keep it VERY short, so it fits on homepage").some, half = true)(form3.input(_))
      ),
      form3.split(
        form3.group(form("homepageHours"), raw(s"Hours on homepage (0 to ${CrudForm.maxHomepageHours})"), half = true, help = raw("Ask first!").some)(form3.input(_, typ = "number")),
        form3.group(form("image"), raw("Custom icon"), half = true)(form3.select(_, CrudForm.imageChoices))
      ),
      form3.group(form("headline"), raw("Homepage headline"), help = raw("Keep it VERY short, so it fits on homepage").some)(form3.input(_)),
      form3.group(form("description"), raw("Full description"), help = raw("Link: [text](url)").some)(form3.textarea(_)(rows := 6)),

      form3.split(
        form3.group(form("hostName"), raw("Host"), half = true) { f =>
          input(
            cls := s"form-control user-autocomplete${limitedEdit ?? " disabled"}",
            required := "required",
            name := f.name,
            id := form3.id(f),
            value := f.value,
            dataTag := "span"
          )
        },
        form3.group(form("arbiterName"), raw("Arbiter (optional)"), half = true) { f =>
          input(
            cls := "form-control user-autocomplete",
            name := f.name,
            id := form3.id(f),
            value := f.value,
            dataTag := "span"
          )
        }
      ),
      limitedEdit option form3.hidden(form("hostName")),
      form3.split(
        div(cls := "form-group form-half"),
        form3.checkbox(form("arbiterHidden"), raw("Hidden arbiter"), help = raw("Arbiter is not visible in the lobby").some, half = true)
      ),

      form3.split(
        form3.group(form("clockTime"), raw("Clock time"), half = true)(form3.select(_, CrudForm.moderatorClockTimeChoices, disabled = limitedEdit)),
        form3.group(form("clockIncrement"), raw("Clock increment"), half = true)(form3.select(_, clockIncrementChoices, disabled = limitedEdit))
      ),
      form3.split(
        form3.group(form("color"), raw("Host color"), half = true)(form3.select(_, colorChoices, disabled = limitedEdit)),
        form3.group(form("clockExtra"), raw("Host extra time"), half = true)(form3.select(_, clockExtraChoices, disabled = limitedEdit))
      ),
      form3.group(form("percentage"), raw("Target winning percentage (optional, min. 50%)"))(form3.input(_, typ = "number")(min := 50, max := 100)),
      form3.group(form("drawLimit"), raw("No draw offers before move (optional, 0 disables draw offers)"))(form3.input(_, typ = "number")(min := 0, max := 99)),
      form3.split(
        form3.group(form("chat"), raw("Chat is available for"), half = true)(form3.select(_, chatChoices)),
        form3.group(form("ceval"), raw("Live computer analysis"), half = true)(form3.select(_, CrudForm.cevalChoices))
      ),
      form3.split(
        form3.checkbox(form("noAssistance"), raw("No playing assistance"), help = raw("Disables premoves, piece destinations and material difference").some, half = true),
        form3.group(form("fmjd"), raw("Official rating visibility"), half = true)(form3.select(_, CrudForm.fmjdChoices))
      ),
      form3.group(form("variants"), raw("Variants")) { _ =>
        setup.filter.renderCheckboxes(form, "variants", form.value.map(f => f.variants.map(_.toString)).getOrElse(Nil), translatedVariantChoicesWithVariants, disabled = limitedEdit)
      },
      form3.action(form3.submit(trans.apply()))
    )
  }

  private def hostAndArbiter(simul: Simul)(implicit ctx: Context) = td(
    userIdLink(simul.hostId.some, withOnline = false), br,
    simul.arbiterId.map { arbiterId =>
      span(cls := "arbiter")(userIdLink(arbiterId.some, withOnline = false))
    }
  )

  def index(simuls: Paginator[Simul])(implicit ctx: Context) = layout(
    title = "Simul manager",
    evenMoreJs = infiniteScrollTag
  ) {
    div(cls := "crud page-menu__content box")(
      div(cls := "box__top")(
        h1("Simul manager"),
        div(cls := "box__top__actions")(
          a(cls := "button button-green", href := routes.SimulCrud.form, dataIcon := "O")
        )
      ),
      table(cls := "slist slist-pad")(
        thead(
          tr(
            th(),
            th("Host / Arbiter"),
            th("Clock"),
            th("Variants"),
            th("UTC Date"),
            th()
          )
        ),
        tbody(cls := "infinitescroll")(
          simuls.nextPage.map { n =>
            frag(
              tr(
                th(cls := "pager none")(
                  a(rel := "next", href := routes.SimulCrud.index(n))("Next")
                )
              ),
              tr()
            )
          },
          simuls.currentPageResults.map { simul =>
            tr(cls := "paginated")(
              td(
                a(href := routes.SimulCrud.edit(simul.id))(strong(simul.fullName), " ", em(simul.spotlight.map(_.headline)))
              ),
              hostAndArbiter(simul),
              td(simul.clock.config.show),
              td(simul.variants.map(_.name).mkString(", ")),
              simul.spotlight.map(spot => td(showDateTimeUTC(spot.startsAt), " ", momentFromNow(spot.startsAt))),
              td(a(href := routes.Simul.show(simul.id), dataIcon := "v", title := "View on site"))
            )
          }
        )
      )
    )
  }
}
