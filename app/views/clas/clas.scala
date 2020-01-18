package views.html.clas

import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.clas.{ Clas, Student }
import lila.clas.ClasForm.ClasData
import controllers.routes

object clas {

  def home(doc: io.prismic.Document, resolver: io.prismic.DocumentLinkResolver)(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = frag(
        cssTag("page"),
        cssTag("clas")
      ),
      title = ~doc.getText("doc.title")
    ) {
      main(cls := "page-small box box-pad page clas-home")(
        h1(doc.getText("doc.title")),
        div(cls := "clas-home__doc")(
          raw(~doc.getHtml("doc.content", resolver))
        ),
        div(cls := "clas-home__onboard")(
          button(cls := "button button-fat disabled", disabled)(
            "Apply for Lichess Teacher"
          ),
          p("Only on invitation for now! We will take applications soon.")
        )
      )
    }

  def index(classes: List[Clas])(implicit ctx: Context) =
    bits.layout("Lichess Classes", Right("classes"))(
      cls := "clas-index",
      div(cls := "box__top")(
        h1("Lichess Classes"),
        a(
          href := routes.Clas.form,
          cls := "new button button-green button-empty",
          title := "New Class",
          dataIcon := "O"
        )
      ),
      if (classes.isEmpty)
        frag(hr, p(cls := "box__pad classes__empty")("No classes yet."))
      else
        div(cls := "classes")(
          classes.map { clas =>
            div(
              cls := List("clas-widget" -> true, "clas-widget-archived" -> clas.isArchived),
              dataIcon := "f"
            )(
              a(cls := "overlay", href := routes.Clas.show(clas.id.value)),
              div(
                h3(clas.name),
                p(clas.desc)
              )
            )
          }
        )
    )

  def showToTeacher(
      clas: Clas,
      students: List[Student.WithUser]
  )(implicit ctx: Context) =
    bits.layout(clas.name, Left(clas withStudents students.map(_.student)))(
      cls := "clas-show",
      div(cls := "box__top")(
        h1(dataIcon := "f", cls := "text")(clas.name),
        div(cls := "box__top__actions")(
          a(
            href := routes.Clas.edit(clas.id.value),
            cls := "button button-empty"
          )("Edit"),
          a(
            href := routes.Clas.studentForm(clas.id.value),
            cls := "button button-green text",
            dataIcon := "O"
          )("Add student")
        )
      ),
      div(cls := "box__pad")(
        standardFlash(),
        clas.archived map { archived =>
          div(cls := "clas-show__archived archived")(
            bits.showArchived(archived),
            postForm(action := routes.Clas.archive(clas.id.value, false))(
              form3.submit("Restore", icon = none)(
                cls := "confirm button-empty",
                title := "Revive the class"
              )
            )
          )
        },
        clas.desc.nonEmpty option div(cls := "clas-desc")(clas.desc),
        teachers(clas)
      ),
      students.partition(_.student.isArchived) match {
        case (archived, active) =>
          frag(
            div(cls := "students")(student.list(clas, active, true)("Students")),
            archived.nonEmpty option div(cls := "students students-archived")(
              student.list(clas, archived, true)("Archived students")
            )
          )
      }
    )

  def showToStudent(
      clas: Clas,
      students: List[Student.WithUser]
  )(implicit ctx: Context) =
    bits.layout(clas.name, Left(clas withStudents Nil))(
      cls := "clas-show",
      div(cls := "box__top")(
        h1(dataIcon := "f", cls := "text")(clas.name)
      ),
      clas.desc.nonEmpty option div(cls := "box__pad clas-desc")(clas.desc),
      clas.archived map { archived =>
        div(cls := "clas-show__archived archived")(bits.showArchived(archived))
      },
      teachers(clas),
      div(cls := "students")(student.list(clas, students, false)("Students"))
    )

  private def teachers(clas: Clas) =
    p(cls := "teachers")(
      "Teachers: ",
      fragList(clas.teachers.toList.map(t => userIdLink(t.value.some)))
    )

  def create(form: Form[ClasData])(implicit ctx: Context) =
    bits.layout("New class", Right("newClass"))(
      cls := "box-pad",
      h1("New class"),
      innerForm(form, none)
    )

  def edit(c: lila.clas.Clas, form: Form[ClasData])(implicit ctx: Context) =
    bits.layout(c.name, Left(c withStudents Nil))(
      cls := "box-pad",
      h1("Edit ", c.name),
      innerForm(form, c.some),
      hr,
      c.isActive option postForm(
        action := routes.Clas.archive(c.id.value, true),
        cls := "clas-edit__archive"
      )(
        form3.submit("Archive", icon = none)(
          cls := "confirm button-red button-empty",
          title := "Disband the class"
        )
      )
    )

  private def innerForm(form: Form[ClasData], clas: Option[Clas])(implicit ctx: Context) =
    postForm(cls := "form3", action := clas.fold(routes.Clas.create())(c => routes.Clas.update(c.id.value)))(
      form3.globalError(form),
      form3.group(form("name"), frag("Class name"))(form3.input(_)(autofocus)),
      form3.group(form("desc"), raw("Class description"))(form3.textarea(_)(rows := 5)),
      form3.actions(
        a(href := clas.fold(routes.Clas.index())(c => routes.Clas.show(c.id.value)))(trans.cancel()),
        form3.submit(trans.apply())
      )
    )
}
