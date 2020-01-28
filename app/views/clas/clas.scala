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
        div(cls := "clas-home__doc body")(
          raw(~doc.getHtml("doc.content", resolver))
        ),
        div(cls := "clas-home__onboard")(
          a(cls := "button button-fat", href := routes.Clas.verifyTeacher)(
            "Apply for Lichess Teacher"
          )
        )
      )
    }

  def teacherIndex(classes: List[Clas])(implicit ctx: Context) =
    bits.layout("Lichess Classes", Right("classes"))(
      cls := "clas-index",
      div(cls := "box__top")(
        h1("Lichess Classes"),
        a(
          href := routes.Clas.form,
          cls := "new button button-empty",
          title := "New Class",
          dataIcon := "O"
        )
      ),
      if (classes.isEmpty)
        frag(hr, p(cls := "box__pad classes__empty")("No classes yet."))
      else
        renderClasses(classes)
    )

  def studentIndex(classes: List[Clas])(implicit ctx: Context) =
    bits.layout("Lichess Classes", Right("classes"))(
      cls := "clas-index",
      div(cls := "box__top")(h1("Lichess Classes")),
      renderClasses(classes)
    )

  private def renderClasses(classes: List[Clas]) =
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

  def teachers(clas: Clas) =
    div(cls := "clas-teachers")(
      "Teachers: ",
      fragList(clas.teachers.toList.map(t => userIdLink(t.value.some)))
    )

  def create(form: Form[ClasData])(implicit ctx: Context) =
    bits.layout("New class", Right("newClass"))(
      cls := "box-pad",
      h1("New class"),
      innerForm(form, none)
    )

  def edit(c: lila.clas.Clas, students: List[Student.WithUser], form: Form[ClasData])(implicit ctx: Context) =
    teacherDashboard.layout(c, students, "edit")(
      div(cls := "box-pad")(
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
    )

  private def innerForm(form: Form[ClasData], clas: Option[Clas])(implicit ctx: Context) =
    postForm(cls := "form3", action := clas.fold(routes.Clas.create())(c => routes.Clas.update(c.id.value)))(
      form3.globalError(form),
      form3.group(form("name"), frag("Class name"))(form3.input(_)(autofocus)),
      form3.group(
        form("desc"),
        frag("Class description"),
        help = frag("Visible by both teachers and students of the class").some
      )(form3.textarea(_)(rows := 5)),
      clas match {
        case None => form3.hidden(form("teachers"), ctx.userId)
        case Some(_) =>
          form3.group(
            form("teachers"),
            frag("Teachers of the class"),
            help = frag(
              "Add Lichess usernames to invite them as teachers. One per line.",
              br,
              "All teachers must be ",
              a(href := routes.Clas.verifyTeacher)("vetted by Lichess"),
              " before being invited."
            ).some
          )(form3.textarea(_)(rows := 4))
      },
      form3.actions(
        a(href := clas.fold(routes.Clas.index())(c => routes.Clas.show(c.id.value)))(trans.cancel()),
        form3.submit(trans.apply())
      )
    )
}
