package views.html.mod

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object permissions {

  def apply(u: lila.user.User)(implicit ctx: Context) =
    views.html.base.layout(
      title = s"${u.username} permissions",
      moreCss = frag(
        cssTag("mod.communication"),
        cssTag("form3")
      ),
      moreJs = embedJsUnsafe("""$(function() {
$('button.clear').on('click', function() {
  $('#permissions option:selected').prop('selected', false);
});});""")
    ) {
      main(id := "permissions", cls := "page-small box box-pad")(
        h1(userLink(u), " permissions"),
        postForm(cls := "form3", action := routes.Mod.permissions(u.username))(
          div(cls := "permission-list")(
            lila.security.Permission.allButSuperAdmin
              .sortBy(_.name)
              .filter { p =>
                ctx.me.exists(canGrant(_, p))
              }
              .map { perm =>
                val id = s"permission-${perm.name}"
                div(
                  span(
                    input(
                      st.id := id,
                      cls := "cmn-toggle",
                      tpe := "checkbox",
                      name := "permissions[]",
                      value := perm.name,
                      u.roles.contains(perm.name) option checked
                    ),
                    label(`for` := id)
                  ),
                  label(`for` := id)(perm.name drop 5)
                )
              }
          ),
          form3.actions(
            button(cls := "button button-red clear", tpe := "button")("Clear"),
            submitButton(cls := "button")("Save")
          )
        )
      )
    }
}
