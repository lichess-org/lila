package lila.event
package ui

import play.api.data.Form

import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class EventUi(helpers: Helpers)(modMenu: Context ?=> Frag):
  import helpers.{ *, given }

  private val dataSeconds = attr("data-seconds")

  private def page(title: String, css: String = "mod.misc")(using Context) =
    Page(title)
      .css(css)
      .js(Esm("bits.flatpickr"))
      .wrap: body =>
        main(cls := "page-menu")(modMenu, body)

  def create(form: Form[?])(using Context) =
    page(title = "New event", css = "mod.form"):
      div(cls := "crud page-menu__content box box-pad")(
        h1(cls := "box__top")("New event"),
        postForm(cls := "form3", action := routes.Event.create)(inForm(form))
      )

  def edit(event: Event, form: Form[?])(using Context) =
    page(title = event.title, css = "mod.form"):
      div(cls := "crud edit page-menu__content box box-pad")(
        boxTop(
          h1(
            a(href := routes.Event.show(event.id))(event.title),
            span("Created by ", titleNameOrId(event.createdBy), " ", momentFromNow(event.createdAt)),
            event.updatedBy.map { updatedBy =>
              span("Updated by ", titleNameOrId(updatedBy), " ", event.updatedAt.map(momentFromNow(_)))
            }
          ),
          st.form(cls := "box__top__actions", action := routes.Event.cloneE(event.id), method := "get")(
            form3.submit("Clone", Icon.Mic.some)(cls := "button-green button-empty")
          )
        ),
        standardFlash,
        postForm(cls := "form3", action := routes.Event.update(event.id))(inForm(form))
      )

  def iconOf(e: Event) =
    e.icon match
      case None => i(cls := "img", dataIcon := Icon.Mic)
      case Some(c) if c == EventForm.icon.broadcast => i(cls := "img", dataIcon := Icon.RadioTower)
      case Some(c) => img(cls := "img", src := assetUrl(s"images/$c"))

  def show(e: Event, description: Option[Html])(using Context) =
    Page(e.title)
      .css("bits.event")
      .js(esmInitBit("eventCountdown")):
        main(cls := "page-small event box box-pad")(
          boxTop(
            iconOf(e),
            div(
              h1(e.title),
              strong(cls := "headline")(e.headline)
            )
          ),
          description.map(div(cls := "desc")(_)),
          if e.isFinished then p(cls := "desc")("The event is finished.")
          else if e.isNow then a(href := e.url, cls := "button button-fat")(trans.site.eventInProgress())
          else
            ul(cls := "countdown", dataSeconds := (~e.secondsToStart + 1)):
              List("Days", "Hours", "Minutes", "Seconds").map: t =>
                li(span(cls := t.toLowerCase), t)
        )

  def manager(events: List[Event])(using Context) =
    val title = "Event manager"
    page(title = title):
      div(cls := "crud page-menu__content box")(
        boxTop(
          h1(title),
          div(cls := "box__top__actions")(
            a(cls := "button button-green", href := routes.Event.form, dataIcon := Icon.PlusButton)
          )
        ),
        table(cls := "slist slist-pad")(
          thead(
            tr(
              th,
              th(utcLink, " start"),
              th(utcLink, " end"),
              th
            )
          ),
          tbody:
            events.map: e =>
              tr(
                td(
                  a(href := routes.Event.edit(e.id))(
                    strong(e.title),
                    em(e.headline)
                  )
                ),
                td(
                  showInstant(e.startsAt),
                  momentFromNow(e.startsAt)
                ),
                td(
                  showInstant(e.finishesAt),
                  momentFromNow(e.finishesAt)
                ),
                td(a(cls := "text", href := routes.Event.show(e.id), dataIcon := Icon.Eye))
              )
        )
      )

  private def inForm(form: Form[?])(using Context) =
    frag(
      form3.split(
        form3.group(form("startsAt"), frag("Start date ", strong(utcLink)), half = true)(
          form3.flatpickr(_, local = true)
        ),
        form3.group(form("finishesAt"), frag("End date ", strong(utcLink)), half = true)(
          form3.flatpickr(_, local = true)
        )
      ),
      form3.split(
        form3.group(
          form("title"),
          raw("Short title"),
          help = raw("Keep it VERY short, so it fits on homepage").some,
          half = true
        )(form3.input(_)),
        form3.group(
          form("icon"),
          frag("Icon"),
          half = true,
          help = frag("Displayed on the homepage button").some
        )(form3.select(_, EventForm.icon.choices))
      ),
      form3.group(
        form("headline"),
        raw("Short headline"),
        help = raw("Keep it VERY short, so it fits on homepage").some
      )(form3.input(_)),
      form3
        .group(form("description"), raw("Possibly long description"), help = raw("Markdown enabled").some)(
          form3.textarea(_)(rows := 15)
        ),
      form3.split(
        form3.group(
          form("url"),
          raw("External URL"),
          help = raw("What to redirect to when the event starts").some,
          half = true
        )(form3.input(_)),
        form3.checkboxGroup(
          form("countdown"),
          frag("Show countdown"),
          help = frag(
            "Show a countdown on the event page before start. Unselect to redirect to the event URL immediately, even before the event has started."
          ).some,
          half = true
        )
      ),
      form3.split(
        form3.group(form("lang"), raw("Language"), half = true):
          form3.select(_, langList.popularLanguagesForm.choices)
        ,
        form3.group(
          form("hostedBy"),
          raw("Hosted by Lichess user"),
          help = raw("Username that must not be featured while the event is ongoing").some,
          half = true
        ): f =>
          div(cls := "complete-parent")(
            input(
              cls := "form-control user-autocomplete",
              name := f.name,
              id := form3.id(f),
              value := f.value,
              dataTag := "span"
            )
          )
      ),
      form3.split(
        form3.checkboxGroup(form("enabled"), "Enabled", help = raw("Display the event").some, half = true),
        form3.group(
          form("homepageHours"),
          raw("Hours on homepage before the start (0 to 24)"),
          half = true,
          help = raw("Go easy on this. The event will also remain on homepage while ongoing.").some
        )(form3.input(_, typ = "number")(step := ".01"))
      ),
      form3.action(form3.submit(trans.site.apply()))
    )
