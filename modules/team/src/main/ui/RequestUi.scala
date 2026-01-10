package lila.team
package ui

import play.api.data.Form
import scalalib.paginator.Paginator

import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class RequestUi(helpers: Helpers, bits: TeamUi):
  import helpers.{ *, given }
  import trans.team as trt
  import bits.{ TeamPage, menu }

  def requestForm(t: Team, form: Form[?])(using Context) =
    TeamPage(s"${trans.team.joinTeam.txt()} ${t.name}"):
      main(cls := "page-menu page-small")(
        menu("requests".some),
        div(cls := "page-menu__content box box-pad")(
          h1(cls := "box__top")(s"${trt.joinTeam.txt()} ${t.name}"),
          div(cls := "team-show__desc")(bits.markdown(t, t.description)),
          postForm(cls := "form3", action := routes.Team.requestCreate(t.id))(
            (!t.open).so(
              frag(
                form3.group(form("message"), trans.site.message())(form3.textarea(_)()),
                p(trt.willBeReviewed())
              )
            ),
            t.password.nonEmpty.so(
              form3.passwordModified(form("password"), trt.entryCode(), reveal = false)(
                autocomplete := "new-password"
              )
            ),
            form3.globalError(form),
            form3.actions(
              a(href := routes.Team.show(t.slug))(trans.site.cancel()),
              form3.submit(trt.joinTeam())
            )
          )
        )
      )

  def all(requests: List[RequestWithUser])(using Context) =
    val title = trans.team.xJoinRequests.pluralSameTxt(requests.size)
    TeamPage(title):
      main(cls := "page-menu")(
        menu("requests".some),
        div(cls := "page-menu__content box box-pad")(
          h1(cls := "box__top")(title),
          list(requests, none)
        )
      )

  def list(requests: List[RequestWithUser], t: Option[Team])(using ctx: Context) =
    table(cls := "slist requests datatable")(
      tbody(
        requests.map: request =>
          tr(
            if t.isEmpty then td(userLink(request.user), " ", teamLink(request.team))
            else td(userLink(request.user)),
            td(request.message),
            td(momentFromNow(request.date)),
            td(cls := "process")(
              postForm(cls := "process-request", action := routes.Team.requestProcess(request.id))(
                form3.hidden("url", t.fold(routes.Team.requests)(te => routes.Team.show(te.id))),
                button(name := "process", cls := "button button-empty button-red", value := "decline")(
                  trans.site.decline()
                ),
                button(name := "process", cls := "button button-green", value := "accept")(
                  trans.site.accept()
                )
              )
            )
          )
      )
    )

  def declined(team: Team, requests: Paginator[RequestWithUser], search: Option[UserStr])(using Context) =
    val title = s"${team.name} • ${trans.team.declinedRequests.txt()}"
    val pager = paginationByQuery(routes.Team.declinedRequests(team.id, 1), requests, showPost = true)
    TeamPage(title).js(Esm("mod.teamAdmin")):
      main(cls := "page-menu page-small")(
        menu(none),
        div(cls := "page-menu__content box box-pad")(
          boxTop(
            h1(
              a(href := routes.Team.show(team.id))(
                team.name
              ),
              " • ",
              trans.team.declinedRequests()
            ),
            st.form(
              cls := "search team-declined-request",
              method := "GET",
              action := routes.Team.declinedRequests(team.id, 1)
            )(
              div(
                input(
                  st.name := "search",
                  value := search,
                  placeholder := trans.search.search.txt()
                ),
                submitButton(cls := "button", dataIcon := Icon.Search)
              )
            )
          ),
          pager,
          table(cls := "slist")(
            tbody(
              requests.currentPageResults.map { request =>
                tr(
                  td(userLink(request.user)),
                  td(request.message),
                  td(momentFromNow(request.date)),
                  td(cls := "process")(
                    postForm(
                      cls := "process-request",
                      action := routes.Team.requestProcess(request.id)
                    )(
                      form3.hidden("url", routes.Team.declinedRequests(team.id, requests.currentPage)),
                      button(name := "process", cls := "button button-green", value := "accept")(
                        trans.site.accept()
                      )
                    )
                  )
                )
              }
            )
          ),
          pager
        )
      )
