package views.html.mod

import controllers.routes

import lila.app.templating.Environment.{ *, given }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.core.perm.Permission

import lila.security.Granter.canGrant
import lila.security.Permission.findGranterPackage

object permissions:

  def apply(u: User)(using ctx: PageContext, me: Me) =
    views.html.base.layout(
      title = s"${u.username} permissions",
      moreCss = frag(
        cssTag("mod.permission"),
        cssTag("form3")
      )
    ):
      main(cls := "mod-permissions page-small box box-pad")(
        boxTop(h1(userLink(u), " permissions")),
        standardFlash,
        postForm(cls := "form3", action := routes.Mod.permissions(u.username))(
          p(cls := "granted")("In green, permissions enabled manually or by a package."),
          div(cls := "permission-list")(
            lila.security.Permission.categorized
              .filter { (_, ps) => ps.exists(canGrant(_)) }
              .map: (categ, perms) =>
                st.section(
                  h2(categ),
                  perms
                    .filter(canGrant)
                    .map: perm =>
                      val id = s"permission-${perm.dbKey}"
                      div(
                        cls := isGranted(perm, u).option("granted"),
                        title := isGranted(perm, u).so {
                          findGranterPackage(Permission(u), perm).map { p =>
                            s"Granted by package: $p"
                          }
                        }
                      )(
                        span(
                          form3.cmnToggle(
                            id,
                            "permissions[]",
                            checked = u.roles.contains(perm.dbKey),
                            value = perm.dbKey
                          )
                        ),
                        label(`for` := id)(perm.name)
                      )
                )
          ),
          form3.actions(
            a(href := routes.User.show(u.username))(trans.site.cancel()),
            submitButton(cls := "button")(trans.site.save())
          )
        )
      )
