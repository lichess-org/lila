package lila.clas
package ui
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class ClasUi(helpers: lila.ui.Helpers)(searchMenu: Context ?=> Frag):
  import helpers.{ *, given }

  def ClasPage(
      title: String,
      active: Either[Clas.WithStudents, String],
      student: Option[Student] = None
  )(mods: AttrPair*)(using lila.ui.Context): Page =
    Page(title)
      .css("bits.clas")
      .js(Esm("bits.clas"))
      .wrap: body =>
        if Granter.opt(_.Teacher) then
          main(cls := "page-menu")(
            teacherMenu(active, student),
            div(cls := "page-menu__content box")(mods, body)
          )
        else main(cls := "page-small box")(mods, body)

  def home(using ctx: Context) =
    Page(trans.clas.lichessClasses.txt())
      .css("bits.page", "bits.clas"):
        main(cls := "page-small box box-pad page clas-home")(
          h1(trans.clas.lichessClasses()),
          div(cls := "clas-home__doc body")(
            p(trans.clas.teachClassesOfChessStudents()),
            h2(trans.clas.features()),
            ul(
              li(trans.clas.quicklyGenerateSafeUsernames()),
              li(trans.clas.trackStudentProgress()),
              li(trans.clas.messageAllStudents()),
              li(trans.clas.freeForAllForever())
            )
          ),
          if ctx.isAuth
          then
            div(cls := "clas-home__onboard")(
              postForm(action := routes.Clas.becomeTeacher)(
                submitButton(cls := "button button-fat")(trans.clas.applyToBeLichessTeacher())
              )
            )
          else
            frag(
              standardFlash,
              div(cls := "clas-home__login")(
                postForm(action := routes.Auth.clasLogin)(
                  input(
                    name := "code",
                    st.placeholder := trans.clas.quickLoginCode.txt(),
                    spellcheck := "false",
                    autocomplete := "off",
                    required
                  ),
                  submitButton(cls := "button button-fat")(trans.site.signIn())
                )
              )
            )
        )

  def showArchived(archived: Clas.Recorded)(using Translate) =
    div(
      trans.clas.removedByX(userIdLink(archived.by.some)),
      " ",
      momentFromNowOnce(archived.at)
    )

  private def teacherMenu(active: Either[Clas.WithStudents, String], student: Option[Student])(using
      Context
  ) =
    lila.ui.bits.pageMenuSubnav(
      a(cls := active.toOption.map(_.active("classes")), href := routes.Clas.index)(
        trans.clas.lichessClasses()
      ),
      active.left.toOption.map { clas =>
        frag(
          a(cls := "active", href := routes.Clas.show(clas.clas.id))(clas.clas.name),
          clas.students.map { s =>
            a(
              cls := List("student" -> true, "active" -> student.exists(s.is)),
              href := routes.Clas.studentShow(clas.clas.id, s.userId)
            )(
              titleNameOrId(s.userId),
              em(s.realName)
            )
          }
        )
      } | {
        a(cls := active.toOption.map(_.active("newClass")), href := routes.Clas.form)(
          trans.clas.newClass()
        )
      }
    )

  object search:

    def clas(c: Clas, userTable: Frag)(using Context) =
      Page("IP address")
        .css("mod.misc")
        .js(Esm("mod.search")):
          main(cls := "page-menu")(
            searchMenu,
            div(cls := "mod-search page-menu__content box")(
              boxTop(
                h1("Class ", a(href := routes.Clas.show(c.id))(c.name)),
                p("Teachers: ", c.teachers.toList.map(id => teacherLink(id)))
              ),
              br,
              br,
              userTable
            )
          )

    def teacher(teacherId: UserId, classes: List[Clas])(using Context) =
      Page("Classes").css("mod.misc"):
        main(cls := "page-menu")(
          searchMenu,
          div(cls := "mod-search page-menu__content box")(
            boxTop(
              h1("Classes from", userIdLink(teacherId.some))
            ),
            br,
            br,
            classes.nonEmpty.option(
              table(cls := "slist slist-pad")(
                thead(
                  tr(
                    th(dataSortAsc)("Id"),
                    th(dataSortAsc)("Name"),
                    th("Created"),
                    th("Archived"),
                    th(dataSortAsc)("Teachers (first is owner)")
                  )
                ),
                tbody(
                  classes.map: c =>
                    tr(
                      td(a(href := routes.Clas.show(c.id))(s"${c.id}")),
                      td(c.name),
                      td(momentFromNow(c.created.at)),
                      c.archived match
                        case None => td("No")
                        case Some(Clas.Recorded(closerId, at)) =>
                          td(userIdLink(closerId.some), nbsp, momentFromNow(at))
                      ,
                      td(c.teachers.toList.map(id => teacherLink(id)))
                    )
                )
              )
            )
          )
        )

    private def teacherLink(userId: UserId)(using Context) =
      lightUserSync(userId).map: user =>
        a(
          href := routes.Clas.teacher(user.name),
          cls := userClass(user.id, none, withOnline = true),
          dataHref := routes.User.show(user.name)
        )(
          lineIcon(user),
          titleTag(user),
          user.name
        )
