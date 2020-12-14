package views.html.team

import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.richText

import controllers.routes

object request {

  import trans.team._

  def requestForm(t: lila.team.Team, form: Form[_])(implicit ctx: Context) = {

    val title = s"${joinTeam.txt()} ${t.name}"

    views.html.base.layout(
      title = title,
      moreCss = cssTag("team")
    ) {
      main(cls := "page-menu page-small")(
        bits.menu("requests".some),
        div(cls := "page-menu__content box box-pad")(
          h1(title),
          p(style := "margin:2em 0")(richText(t.description)),
          postForm(cls := "form3", action := routes.Team.requestCreate(t.id))(
            form3.group(form("message"), trans.message())(form3.textarea(_)()),
            form3.group(form("password"), teamPassword(), help = teamPasswordDescriptionForRequester().some)(
              form3.input(_)
            ),
            p(willBeReviewed()),
            form3.actions(
              a(href := routes.Team.show(t.slug))(trans.cancel()),
              form3.submit(joinTeam())
            )
          )
        )
      )
    }
  }

  def all(requests: List[lila.team.RequestWithUser])(implicit ctx: Context) = {
    val title = xJoinRequests.pluralSameTxt(requests.size)
    bits.layout(title = title) {
      main(cls := "page-menu")(
        bits.menu("requests".some),
        div(cls := "page-menu__content box box-pad")(
          h1(title),
          list(requests, none)
        )
      )
    }
  }

  private[team] def list(requests: List[lila.team.RequestWithUser], t: Option[lila.team.Team])(implicit
      ctx: Context
  ) =
    table(cls := "slist requests @if(t.isEmpty){all}else{for-team} datatable")(
      tbody(
        requests.map { request =>
          tr(
            if (t.isEmpty) td(userLink(request.user), " ", teamLink(request.team))
            else td(userLink(request.user)),
            td(richText(request.message)),
            td(momentFromNow(request.date)),
            td(cls := "process")(
              postForm(cls := "process-request", action := routes.Team.requestProcess(request.id))(
                input(
                  tpe := "hidden",
                  name := "url",
                  value := t.fold(routes.Team.requests())(te => routes.Team.show(te.id))
                ),
                button(name := "process", cls := "button button-empty button-red", value := "decline")(
                  trans.decline()
                ),
                button(name := "process", cls := "button button-green", value := "accept")(trans.accept())
              )
            )
          )
        }
      )
    )
}
