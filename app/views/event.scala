package views.html

import controllers.routes
import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.richText

object event {

  private val dataSeconds = attr("data-seconds")

  def create(form: Form[_])(implicit ctx: Context) =
    layout(title = "New event", css = "mod.form") {
      div(cls := "crud page-menu__content box box-pad")(
        h1("New event"),
        postForm(cls := "content_box_content form3", action := routes.Event.create())(inForm(form))
      )
    }

  def edit(event: lila.event.Event, form: Form[_])(implicit ctx: Context) =
    layout(title = event.title, css = "mod.form") {
      div(cls := "crud edit page-menu__content box box-pad")(
        div(cls := "box__top")(
          h1(
            a(href := routes.Event.show(event.id))(event.title),
            span("Created by ", usernameOrId(event.createdBy.value), " ", momentFromNow(event.createdAt))
          ),
          st.form(cls := "box__top__actions", action := routes.Event.cloneE(event.id), method := "get")(
            form3.submit("Clone", "".some, klass = "button-green")
          )
        ),
        standardFlash(),
        postForm(cls := "content_box_content form3", action := routes.Event.update(event.id))(inForm(form))
      )
    }

  def show(e: lila.event.Event)(implicit ctx: Context) =
    views.html.base.layout(
      title = e.title,
      moreCss = cssTag("event"),
      moreJs = jsTag("event-countdown.js")
    ) {
      main(cls := "page-small event box box-pad")(
        h1(dataIcon := "", cls := "text")(e.title),
        h2(cls := "headline")(e.headline),
        e.description.map { d =>
          p(cls := "desc")(richText(d))
        },
        if (e.isFinished) p(cls := "desc")("The event is finished.")
        else if (e.isNow) a(href := e.url, cls := "button button-fat")(trans.eventInProgress())
        else
          ul(cls := "countdown", dataSeconds := (~e.secondsToStart + 1))(
            List("Days", "Hours", "Minutes", "Seconds") map { t =>
              li(span(cls := t.toLowerCase), t)
            }
          )
      )
    }

  def manager(events: List[lila.event.Event])(implicit ctx: Context) = {
    val title = "Event manager"
    layout(title = title) {
      div(cls := "crud page-menu__content box")(
        div(cls := "box__top")(
          h1(title),
          div(cls := "box__top__actions")(
            a(cls := "button button-green", href := routes.Event.form(), dataIcon := "O")
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
          tbody(
            events.map { e =>
              tr(
                td(
                  a(href := routes.Event.edit(e.id))(
                    strong(e.title),
                    em(e.headline)
                  )
                ),
                td(
                  showDateTimeUTC(e.startsAt),
                  momentFromNow(e.startsAt)
                ),
                td(
                  showDateTimeUTC(e.finishesAt),
                  momentFromNow(e.finishesAt)
                ),
                td(a(cls := "text", href := routes.Event.show(e.id), dataIcon := "v"))
              )
            }
          )
        )
      )
    }
  }

  private def inForm(form: Form[_])(implicit ctx: Context) =
    frag(
      form3.split(
        form3.group(form("startsAt"), frag("Start date ", strong(utcLink)), half = true)(form3.flatpickr(_)),
        form3.group(form("finishesAt"), frag("End date ", strong(utcLink)), half = true)(form3.flatpickr(_))
      ),
      form3.group(
        form("title"),
        raw("Short title"),
        help = raw("Keep it VERY short, so it fits on homepage").some
      )(form3.input(_)),
      form3.group(
        form("headline"),
        raw("Short headline"),
        help = raw("Keep it VERY short, so it fits on homepage").some
      )(form3.input(_)),
      form3
        .group(form("description"), raw("Possibly long description"), help = raw("Link: [text](url)").some)(
          form3.textarea(_)()
        ),
      form3.group(
        form("url"),
        raw("External URL"),
        help = raw("What to redirect to when the event starts").some
      )(form3.input(_)),
      form3.split(
        form3.group(form("lang"), raw("Language"), half = true)(form3.select(_, lila.i18n.LangList.choices)),
        form3.group(
          form("hostedBy"),
          raw("Hosted by Lichess user"),
          help = raw("Username that must not be featured while the event is ongoing").some,
          half = true
        ) { f =>
          input(
            cls := "form-control user-autocomplete",
            name := f.name,
            id := form3.id(f),
            value := f.value,
            dataTag := "span"
          )
        }
      ),
      form3.split(
        form3.checkbox(form("enabled"), raw("Enabled"), help = raw("Display the event").some, half = true),
        form3.group(
          form("homepageHours"),
          raw("Hours on homepage (0 to 24)"),
          half = true,
          help = raw("Ask on slack first!").some
        )(form3.input(_, typ = "number"))
      ),
      form3.action(form3.submit(trans.apply()))
    )

  private def layout(title: String, css: String = "mod.misc")(body: Frag)(implicit ctx: Context) =
    views.html.base.layout(
      title = title,
      moreCss = cssTag(css),
      moreJs = frag(
        flatpickrTag,
        delayFlatpickrStartUTC
      )
    ) {
      main(cls := "page-menu")(
        mod.menu("event"),
        body
      )
    }
}
