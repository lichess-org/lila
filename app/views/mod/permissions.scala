package views.html.mod

import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object permissions {

  def apply(u: lila.user.User)(implicit ctx: Context) =
    views.html.base.layout(
      title = s"${u.username} permissions",
      moreCss = frag(
        responsiveCssTag("mod.communication"),
        responsiveCssTag("form3")
      ),
      moreJs = embedJs("""$(function() {
$('button.clear').on('click', function() {
  $('#permissions option:selected').prop('selected', false);
});});""")
    ) {
        main(id := "permissions", cls := "page-small box box-pad")(
          h1(userLink(u), " permissions"),
          p("Use Ctrl+click to select multiple permissions"),
          form(cls := "form3", action := routes.Mod.permissions(u.username), method := "post")(
            select(name := "permissions[]", multiple := true)(
              lila.security.Permission.allButSuperAdmin.sortBy(_.name).map { p =>
                option(
                  value := p.name,
                  selected := u.roles.contains(p.name).option(true),
                  title := p.children.mkString(", ")
                )(p.toString)
              }
            ),
            form3.actions(
              button(cls := "button button-red clear", tpe := "button")("Clear"),
              button(cls := "button", tpe := "submit")("Save")
            )
          )
        )
      }
}
