package views.html.relay

import play.api.data.Form

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._

import controllers.routes

object form {

  def create(form: Form[_])(implicit ctx: Context) =
    layout("New live broadcast")(
      h1("New live broadcast"),
      inner(form, routes.Relay.create)
    )

  def edit(r: lidraughts.relay.Relay, form: Form[_])(implicit ctx: Context) =
    layout(r.name)(
      h1("Edit ", r.name),
      inner(form, routes.Relay.update(r.slug, r.id.value))
    )

  private def layout(title: String)(body: Modifier*)(implicit ctx: Context) =
    views.html.base.layout(
      title = title,
      moreCss = cssTag("relay.form"),
      moreJs = frag(
        flatpickrTag,
        delayFlatpickrStart
      )
    )(
        main(cls := "page-small box box-pad")(body)
      )

  private def inner(form: Form[_], url: play.api.mvc.Call)(implicit ctx: Context) =
    postForm(cls := "form3", action := url)(
      form3.group(form("name"), frag("Event name"))(form3.input(_)(autofocus)),
      form3.group(form("description"), raw("Short event description"))(form3.textarea(_)(rows := 2)),
      form3.group(form("markup"), raw("Full event description"), help = raw("""<a href="https://guides.github.com/features/mastering-markdown/" target="_blank">Markdown</a> is available""").some)(form3.textarea(_)(rows := 10)),
      if (isGranted(_.Admin)) form3.split(
        form3.checkbox(form("official"), raw("Official lidraughts broadcast"), half = true, help = raw("Feature on /broadcast - for admins only").some),
        form3.group(form("homepageHours"), raw(s"Hours on homepage (0 to ${lidraughts.relay.RelayForm.maxHomepageHours})"), half = true, help = raw("Ask first!").some)(form3.input(_, typ = "number"))
      )
      else frag(
        form3.hidden(form("official")),
        form3.hidden(form("homepageHours"))
      ),
      form3.group(form("syncUrl"), raw("Source URL"))(form3.input(_, typ = "url")),
      if (isGranted(_.Admin)) form3.split(
        form3.group(form("gameIndices"), raw("Game indices"), half = true, help = raw("For broadcasts without index - comma separated game indices").some)(form3.input(_)),
        form3.group(form("simulId"), raw("Simul ID"), half = true, help = raw("Internal use only - overrides source url").some)(form3.input(_))
      )
      else frag(
        form3.hidden(form("gameIndices")),
        form3.hidden(form("simulId"))
      ),
      form3.split(
        form3.group(form("startsAt"), frag(
          "Start date ", strong(utcLink)
        ), help = raw("Optional, if you know when the event starts").some, half = true)(form3.flatpickr(_)),
        if (isGranted(_.Admin))
          form3.group(form("throttle"), raw("Throttle in seconds"), help = raw("Optional, to manually throttle requests. Min 2s, max 60s.").some, half = true)(form3.input(_, typ = "number"))
        else form3.hidden(form("throttle"))
      ),
      if (isGranted(_.Admin)) form3.group(form("credit"), raw("Credit the source"), half = true)(form3.input(_))
      else form3.hidden(form("credit")),
      form3.actions(
        a(href := routes.Relay.index(1))(trans.cancel()),
        form3.submit(trans.apply())
      )
    )
}
