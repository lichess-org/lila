package views.html.team

import controllers.routes
import play.api.data.Form

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }

object request:

  import trans.team.*

  def requestForm(t: lila.team.Team, form: Form[?])(using PageContext) =

    val title = s"${joinTeam.txt()} ${t.name}"

    views.html.base.layout(
      title = title,
      moreCss = cssTag("team")
    ) {
      main(cls := "page-menu page-small")(
        bits.menu("requests".some),
        div(cls := "page-menu__content box box-pad")(
          h1(cls := "box__top")(title),
          div(cls := "team-show__desc")(bits.markdown(t, t.description)),
          postForm(cls := "form3", action := routes.Team.requestCreate(t.id))(
            !t.open so frag(
              form3.group(form("message"), trans.message())(form3.textarea(_)()),
              p(willBeReviewed())
            ),
            t.password.nonEmpty so form3.passwordModified(form("password"), entryCode())(
              autocomplete := "new-password"
            ),
            form3.globalError(form),
            form3.actions(
              a(href := routes.Team.show(t.slug))(trans.cancel()),
              form3.submit(joinTeam())
            )
          )
        )
      )
    }

  def all(requests: List[lila.team.RequestWithUser])(using PageContext) =
    val title = xJoinRequests.pluralSameTxt(requests.size)
    bits.layout(title = title) {
      main(cls := "page-menu")(
        bits.menu("requests".some),
        div(cls := "page-menu__content box box-pad")(
          h1(cls := "box__top")(title),
          list(requests, none)
        )
      )
    }

  private[team] def list(requests: List[lila.team.RequestWithUser], t: Option[lila.team.Team])(using
      ctx: PageContext
  ) =
    table(cls := "slist requests @if(t.isEmpty){all}else{for-team} datatable")(
      tbody(
        requests.map { request =>
          tr(
            if t.isEmpty then td(userLink(request.user), " ", teamLink(request.team))
            else td(userLink(request.user)),
            td(request.message),
            td(momentFromNow(request.date)),
            td(cls := "process")(
              postForm(cls := "process-request", action := routes.Team.requestProcess(request.id))(
                input(
                  tpe   := "hidden",
                  name  := "url",
                  value := t.fold(routes.Team.requests)(te => routes.Team.show(te.id))
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
