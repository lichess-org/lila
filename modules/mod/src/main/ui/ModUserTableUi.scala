package lila.mod
package ui

import lila.ui.*
import ScalatagsTemplate.{ *, given }
import lila.user.WithPerfsAndEmails

object ModUserTableUi:

  val sortNoneTh = th(attr("data-sort-method") := "none")
  val dataSort   = attr("data-sort")
  val email      = tag("email")
  val mark       = tag("marked")

  def canCloseAlt(using me: Option[Me]): Boolean = me.soUse(lila.mod.canCloseAlt)

  def selectAltAll(using Context) = canCloseAlt.option:
    sortNoneTh(
      select(style := "width: 2em")(
        st.option(value := "")(""),
        st.option(value := "all")("Select all"),
        st.option(value := "none")("Select none"),
        st.option(value := "alt")("Alt selected")
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

final class ModUserTableUi(helpers: Helpers, modUi: ModUi):
  import helpers.{ *, given }
  import ModUserTableUi.*

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
            selectAltAll
          )
        ),
        tbody:
          users.map { case lila.user.WithPerfsAndEmails(u, emails) =>
            tr(
              if showUsernames || canViewAltUsername(u.user)
              then
                td(dataSort := u.id)(
                  userLink(u.user, withPerfRating = u.perfs.some, params = "?mod"),
                  Granter.opt(_.Admin).option(email(emails.strList.mkString(", ")))
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
                    modUi.gdprEraseButton(u)(cls := "button button-red button-empty confirm")
                )
              ),
              userCheckboxTd(u.marks.alt)
            )
          }
      )
    )
