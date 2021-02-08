package views.html.clas

import play.api.data.Form
import play.api.i18n.Lang

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.clas.{ Clas, Student }
import lila.clas.ClasForm.ClasData
import controllers.routes

object clas {

  def home(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = frag(
        cssTag("page"),
        cssTag("clas")
      ),
      title = trans.clas.lichessClasses.txt()
    ) {
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
        div(cls := "clas-home__onboard")(
          postForm(action := routes.Clas.becomeTeacher)(
            submitButton(cls := "button button-fat")(trans.clas.applyToBeLichessTeacher())
          )
        )
      )
    }

  def teacherIndex(classes: List[Clas])(implicit ctx: Context) =
    bits.layout(trans.clas.lichessClasses.txt(), Right("classes"))(
      cls := "clas-index",
      div(cls := "box__top")(
        h1(trans.clas.lichessClasses()),
        a(
          href := routes.Clas.form,
          cls := "new button button-empty",
          title := trans.clas.newClass.txt(),
          dataIcon := "O"
        )
      ),
      if (classes.isEmpty)
        frag(hr, p(cls := "box__pad classes__empty")(trans.clas.noClassesYet()))
      else
        renderClasses(classes)
    )

  def studentIndex(classes: List[Clas])(implicit ctx: Context) =
    bits.layout(trans.clas.lichessClasses.txt(), Right("classes"))(
      cls := "clas-index",
      div(cls := "box__top")(h1(trans.clas.lichessClasses())),
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

  def teachers(clas: Clas)(implicit lang: Lang) =
    div(cls := "clas-teachers")(
      trans.clas.teachersX(
        fragList(clas.teachers.toList.map(t => userIdLink(t.some)))
      )
    )

  def create(form: Form[ClasData])(implicit ctx: Context) =
    bits.layout(trans.clas.newClass.txt(), Right("newClass"))(
      cls := "box-pad",
      h1(trans.clas.newClass()),
      innerForm(form, none)
    )

  def edit(c: lila.clas.Clas, students: List[Student.WithUser], form: Form[ClasData])(implicit ctx: Context) =
    teacherDashboard.layout(c, students, "edit")(
      div(cls := "box-pad")(
        innerForm(form, c.some),
        hr,
        c.isActive option postForm(
          action := routes.Clas.archive(c.id.value, v = true),
          cls := "clas-edit__archive"
        )(
          form3.submit(trans.clas.closeClass(), icon = none)(
            cls := "confirm button-red button-empty"
          )
        )
      )
    )

  def notify(c: lila.clas.Clas, students: List[Student.WithUser], form: Form[_])(implicit ctx: Context) =
    teacherDashboard.layout(c, students, "wall")(
      div(cls := "box-pad clas-wall__edit")(
        p(
          strong(trans.clas.sendAMessage()),
          br,
          trans.clas.aLinkToTheClassWillBeAdded()
        ),
        postForm(cls := "form3", action := routes.Clas.notifyPost(c.id.value))(
          form3.globalError(form),
          form3.group(
            form("text"),
            frag(trans.message())
          )(form3.textarea(_)(rows := 3)),
          form3.actions(
            a(href := routes.Clas.wall(c.id.value))(trans.cancel()),
            form3.submit(trans.send())
          )
        )
      )
    )

  private def innerForm(form: Form[ClasData], clas: Option[Clas])(implicit ctx: Context) =
    postForm(cls := "form3", action := clas.fold(routes.Clas.create)(c => routes.Clas.update(c.id.value)))(
      form3.globalError(form),
      form3.group(form("name"), trans.clas.className())(form3.input(_)(autofocus)),
      form3.group(
        form("desc"),
        frag(trans.clas.classDescription()),
        help = trans.clas.visibleByBothStudentsAndTeachers().some
      )(form3.textarea(_)(rows := 5)),
      clas match {
        case None => form3.hidden(form("teachers"), ctx.userId)
        case Some(_) =>
          form3.group(
            form("teachers"),
            trans.clas.teachersOfTheClass(),
            help = frag(
              trans.clas.addLichessUsernames(),
              br,
              trans.clas.theyMustFirstApply()
            ).some
          )(form3.textarea(_)(rows := 4))
      },
      form3.actions(
        a(href := clas.fold(routes.Clas.index)(c => routes.Clas.show(c.id.value)))(trans.cancel()),
        form3.submit(trans.apply())
      )
    )
}
