package views.html.relay

import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object form {

  def create(form: Form[_])(implicit ctx: Context) =
    layout("New live broadcast")(
      h1("New live broadcast"),
      inner(form, routes.Relay.create)
    )

  def edit(r: lila.relay.Relay, form: Form[_])(implicit ctx: Context) =
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
    st.form(cls := "form3", action := url, method := "POST")(
      form3.group(form("name"), frag("Event name"))(form3.input(_)(autofocus)),
      form3.group(form("description"), raw("Event description"))(form3.textarea(_)(rows := 6)),
      if (isGranted(_.Relay))
        form3.checkbox(form("official"), raw("Official lichess broadcast"), help = raw("Feature on /broadcast - for admins only").some)
      else form3.hidden(form("official")),
      form3.group(form("syncUrl"), raw("Source URL"))(form3.input(_, typ = "url")),
      form3.split(
        form3.group(form("startsAt"), frag(
          "Start date ", strong(utcLink)
        ), help = raw("Optional, if you know when the event starts").some, half = true)(form3.flatpickr(_)),
        isGranted(_.Relay) option
          form3.group(form("throttle"), raw("Throttle in seconds"), help = raw("Optional, to manually throttle requests. Min 2s, max 60s.").some, half = true)(form3.input(_, typ = "number"))
      ),
      isGranted(_.Relay) option form3.group(form("credit"), raw("Credit the source"))(form3.input(_)),
      form3.actions(
        a(href := routes.Relay.index(1))(trans.cancel()),
        form3.submit(trans.apply())
      )
    )
}
