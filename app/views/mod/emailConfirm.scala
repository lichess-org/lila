package views.html.mod

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }

import controllers.routes

object emailConfirm:

  def apply(query: String, user: Option[lila.user.User.WithPerfs], email: Option[lila.common.EmailAddress])(
      using ctx: PageContext
  ) =
    views.html.base.layout(
      title = "Email confirmation",
      moreCss = cssTag("mod.misc"),
      moreJs = embedJsUnsafeLoadThen("""$('.mod-confirm form input').on('paste', function() {
setTimeout(function() { $(this).parent().submit(); }.bind(this), 50);
}).each(function() {
this.setSelectionRange(this.value.length, this.value.length);
});""")
    ) {
      main(cls := "page-menu")(
        views.html.mod.menu("email"),
        div(cls := "mod-confirm page-menu__content box box-pad")(
          h1(cls := "box__top")("Confirm a user email"),
          p(
            "If you provide an email, it will confirm the corresponding account, if any.",
            br,
            "If you provide an email and a username, it will set the email to that user, ",
            "but only if the user has not yet confirmed their email."
          ),
          st.form(cls := "search", action := routes.Mod.emailConfirm, method := "GET")(
            input(name := "q", placeholder := "<email> <username (optional)>", value := query, autofocus)
          ),
          user.map: u =>
            table(cls := "slist")(
              thead:
                tr(
                  th("User"),
                  th("Email"),
                  th("Games"),
                  th("Marks"),
                  th("Created"),
                  th("Active"),
                  th("Confirmed")
                )
              ,
              tbody:
                tr(
                  td(userLink(u.user, withPerfRating = u.perfs.some, params = "?mod")),
                  td(email.fold("-")(_.value)),
                  td(u.count.game.localize),
                  td(
                    u.marks.engine option "ENGINE",
                    u.marks.boost option "BOOSTER",
                    u.marks.troll option "SHADOWBAN",
                    u.enabled.no option "CLOSED"
                  ),
                  td(momentFromNow(u.createdAt)),
                  td(u.seenAt.map(momentFromNow(_))),
                  td(style := "font-size:2em")(
                    if !u.everLoggedIn then iconTag(licon.Checkmark)(cls := "is-green")
                    else iconTag(licon.X)(cls                            := "is-red")
                  )
                )
            )
        )
      )
    }
