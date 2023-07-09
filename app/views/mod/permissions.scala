package views.html.mod

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.user.{ Me, User }
import lila.security.Permission

import controllers.routes

object permissions:

  def apply(u: User)(using ctx: PageContext, me: Me) =
    views.html.base.layout(
      title = s"${u.username} permissions",
      moreCss = frag(
        cssTag("mod.permission"),
        cssTag("form3")
      )
    ):
      val userPerms = Permission(u.roles)
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
                        cls := isGranted(perm, u) option "granted",
                        title := isGranted(perm, u).so {
                          Permission.findGranterPackage(userPerms, perm).map { p =>
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
            a(href := routes.User.show(u.username))(trans.cancel()),
            submitButton(cls := "button")(trans.save())
          )
        )
      )
