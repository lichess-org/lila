package views.html.mod

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment.{ *, given }
import lila.ui.ScalatagsTemplate.{ *, given }
import lila.core.perm.Granter
import lila.user.WithPerfsAndEmails

object userTable:

  val sortNoneTh = th(attr("data-sort-method") := "none")
  val dataSort   = attr("data-sort")
  val email      = tag("email")
  val mark       = tag("marked")

  def selectAltAll(using Context) = canCloseAlt.option(
    sortNoneTh(
      select(style := "width: 2em")(
        st.option(value := "")(""),
        st.option(value := "all")("Select all"),
        st.option(value := "none")("Select none"),
        st.option(value := "alt")("Alt selected")
      )
    )
  )

  def userCheckboxTd(isAlt: Boolean)(using Context) = canCloseAlt.option(td:
    input(
      tpe      := "checkbox",
      name     := "user[]",
      st.value := "all",
      disabled := isAlt.option(true)
    )
  )

  def apply(
      users: List[WithPerfsAndEmails],
      showUsernames: Boolean = false,
      eraseButton: Boolean = false
  )(using Context, Me) =
    users.nonEmpty.option(
      table(cls := "slist slist-pad mod-user-table")(
        thead(
          tr(
            th("User"),
            thSortNumber("Games"),
            th("Marks"),
            th("Closed"),
            th("Created"),
            th("Active"),
            eraseButton.option(th),
            userTable.selectAltAll
          )
        ),
        tbody:
          users.map { case lila.user.WithPerfsAndEmails(u, emails) =>
            tr(
              if showUsernames || lila.security.Granter.canViewAltUsername(u.user)
              then
                td(dataSort := u.id)(
                  userLink(u.user, withPerfRating = u.perfs.some, params = "?mod"),
                  isGranted(_.Admin).option(email(emails.strList.mkString(", ")))
                )
              else td,
              td(dataSort := u.count.game)(u.count.game.localize),
              td(
                u.marks.alt.option(mark("ALT")),
                u.marks.engine.option(mark("ENGINE")),
                u.marks.boost.option(mark("BOOSTER")),
                u.marks.troll.option(mark("SHADOWBAN"))
              ),
              td(u.enabled.no.option(mark("CLOSED"))),
              td(dataSort := u.createdAt.toMillis)(momentFromNowServer(u.createdAt)),
              td(dataSort := u.seenAt.map(_.toMillis.toString))(u.seenAt.map(momentFromNowServer)),
              eraseButton.option(
                td(
                  postForm(action := routes.Mod.gdprErase(u.username)):
                    views.html.user.mod.gdprEraseButton(u)(cls := "button button-red button-empty confirm")
                )
              ),
              userTable.userCheckboxTd(u.marks.alt)
            )
          }
      )
    )
