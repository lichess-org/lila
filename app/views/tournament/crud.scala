package views.html
package tournament

import play.api.data.Form

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.common.paginator.Paginator
import lidraughts.rating.PerfType
import lidraughts.tournament.crud.CrudForm
import lidraughts.tournament.{ DataForm, Tournament }

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
          views.html.mod.menu("tour"),
          body
        )
      }

  def create(form: Form[_])(implicit ctx: Context) = layout(
    title = "New tournament",
    css = "mod.form",
    evenMoreJs = jsTag("tournamentForm.js")
  ) {
      div(cls := "crud page-menu__content box box-pad")(
        h1("New tournament"),
        postForm(cls := "form3", action := routes.TournamentCrud.create)(inForm(form))
      )
    }

  def edit(tour: Tournament, form: Form[_])(implicit ctx: Context) = layout(
    title = tour.fullName,
    css = "mod.form",
    evenMoreJs = jsTag("tournamentForm.js")
  ) {
      div(cls := "crud edit page-menu__content box box-pad")(
        div(cls := "box__top")(
          h1(
            a(href := routes.Tournament.show(tour.id))(tour.fullName),
            " ",
            span("Created by ", usernameOrId(tour.createdBy), " on ", showDate(tour.createdAt))
          ),
          st.form(cls := "box__top__actions", action := routes.TournamentCrud.clone(tour.id), method := "get")(
            form3.submit("Clone", "g".some, klass = "button-green")
          )
        ),
        postForm(cls := "form3", action := routes.TournamentCrud.update(tour.id))(inForm(form))
      )
    }

  private def inForm(form: Form[_])(implicit ctx: Context) = frag(
    form3.split(
      form3.group(form("date"), frag("Start date ", strong(utcLink)), half = true)(form3.flatpickr(_)),
      form3.group(form("name"), raw("Name"), help = raw("Keep it VERY short, so it fits on homepage").some, half = true)(form3.input(_))
    ),
    form3.split(
      form3.group(form("homepageHours"), raw(s"Hours on homepage (0 to ${CrudForm.maxHomepageHours})"), half = true, help = raw("Ask first!").some)(form3.input(_, typ = "number")),
      form3.group(form("image"), raw("Custom icon"), half = true)(form3.select(_, CrudForm.imageChoices))
    ),
    form3.group(form("headline"), raw("Homepage headline"), help = raw("Keep it VERY short, so it fits on homepage").some)(form3.input(_)),
    form3.group(form("description"), raw("Full description"), help = raw("Link: [text](url)").some)(form3.textarea(_)(rows := 6)),

    form3.split(
      form3.group(form("variant"), raw("Variant"), half = true) { f =>
        form3.select(f, translatedVariantChoicesWithVariants.map(x => x._1 -> x._2))
      },
      form3.group(form("minutes"), raw("Duration in minutes"), half = true)(form3.input(_, typ = "number"))
    ),
    form3.split(
      form3.group(form("clockTime"), raw("Clock time"), half = true)(form3.select(_, DataForm.clockTimeChoices)),
      form3.group(form("clockIncrement"), raw("Clock increment"), half = true)(form3.select(_, DataForm.clockIncrementChoices))
    ),
    startPosition(form, draughts.variant.Standard),
    startPosition(form, draughts.variant.Russian),

    hr,
    h2("Conditions of entry"),
    tournament.form.condition(form, auto = false, Nil),
    form3.group(form("password"), raw("Password (optional)"))(form3.input(_)),
    form3.action(form3.submit(trans.apply()))
  )

  private def startPosition(form: Form[_], v: draughts.variant.Variant)(implicit ctx: Context) =
    form3.group(form("position_" + v.key), trans.startPosition(), klass = "position-" + v.key)(
      views.html.tournament.form.startingPosition(_, v)
    )

  def index(tours: Paginator[Tournament])(implicit ctx: Context) = layout(
    title = "Tournament manager",
    evenMoreJs = infiniteScrollTag
  ) {
    div(cls := "crud page-menu__content box")(
      div(cls := "box__top")(
        h1("Tournament manager"),
        div(cls := "box__top__actions")(
          a(cls := "button button-green", href := routes.TournamentCrud.form, dataIcon := "O")
        )
      ),
      table(cls := "slist slist-pad")(
        thead(
          tr(
            th(),
            th("Variant"),
            th("Clock"),
            th("Duration"),
            th(utcLink, " Date"),
            th()
          )
        ),
        tbody(cls := "infinitescroll")(
          tours.nextPage.map { n =>
            frag(
              tr(
                th(cls := "pager none")(
                  a(rel := "next", href := routes.TournamentCrud.index(n))("Next")
                )
              ),
              tr()
            )
          },
          tours.currentPageResults.map { tour =>
            tr(cls := "paginated")(
              td(
                a(href := routes.TournamentCrud.edit(tour.id))(strong(tour.fullName), " ", em(tour.spotlight.map(_.headline)))
              ),
              td(tour.variant.name),
              td(tour.clock.toString),
              td(tour.minutes, "m"),
              td(showDateTimeUTC(tour.startsAt), " ", momentFromNow(tour.startsAt)),
              td(a(href := routes.Tournament.show(tour.id), dataIcon := "v", title := "View on site"))
            )
          }
        )
      )
    )
  }
}
