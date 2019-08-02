package views.html.mod

import play.api.data.Form

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._

import controllers.routes

object permissions {

  def apply(u: lidraughts.user.User)(implicit ctx: Context) =
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
          p("Use Ctrl+click to select multiple permissions"),
          postForm(cls := "form3", action := routes.Mod.permissions(u.username))(
            select(name := "permissions[]", multiple)(
              lidraughts.security.Permission.allButSuperAdmin.sortBy(_.name).flatMap { p =>
                ctx.me.exists(canGrant(_, p)) option option(
                  value := p.name,
                  u.roles.contains(p.name) option selected,
                  title := p.children.mkString(", ")
                )(p.toString)
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
