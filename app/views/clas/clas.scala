package views.html.clas

import controllers.clas.routes.{ Clas as clasRoutes }
import play.api.data.Form
import play.api.i18n.Lang

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.clas.{ Clas, Student }
import lila.clas.ClasForm.ClasData

object clas:

  def home(using PageContext) =
    views.html.base.layout(
      moreCss = frag(
        cssTag("page"),
        cssTag("clas")
      ),
      title = trans.clas.lichessClasses.txt()
    ) {
      main(cls := "page-small box box-pad page clas-home")(
        h1(cls := "box__top")(trans.clas.lichessClasses()),
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
          postForm(action := clasRoutes.becomeTeacher)(
            submitButton(cls := "button button-fat")(trans.clas.applyToBeLichessTeacher())
          )
        )
      )
    }

  def teacherIndex(classes: List[Clas], closed: Boolean)(using PageContext) =
    val (active, archived) = classes.partition(_.isActive)
    val (current, others)  = if closed then (archived, active) else (active, archived)
    bits.layout(trans.clas.lichessClasses.txt(), Right("classes"))(
      cls := "clas-index",
      div(cls := "box__top")(
        h1(cls := "box__top")(trans.clas.lichessClasses()),
        a(
          href     := clasRoutes.form,
          cls      := "new button button-empty",
          title    := trans.clas.newClass.txt(),
          dataIcon := licon.PlusButton
        )
      ),
      if current.isEmpty then frag(hr, p(cls := "box__pad classes__empty")(trans.clas.noClassesYet()))
      else renderClasses(current),
      (closed || others.nonEmpty) option div(cls := "clas-index__others")(
        a(href := s"${clasRoutes.index}?closed=${!closed}")(
          "And ",
          others.size.localize,
          " ",
          if closed then "active" else "archived",
          " classes"
        )
      )
    )

  def studentIndex(classes: List[Clas])(using PageContext) =
    bits.layout(trans.clas.lichessClasses.txt(), Right("classes"))(
      cls := "clas-index",
      boxTop(h1(trans.clas.lichessClasses())),
      renderClasses(classes)
    )

  private def renderClasses(classes: List[Clas]) =
    div(cls := "classes")(
      classes.map { clas =>
        div(
          cls      := List("clas-widget" -> true, "clas-widget-archived" -> clas.isArchived),
          dataIcon := licon.Group
        )(
          a(cls := "overlay", href := clasRoutes.show(clas.id.value)),
          div(
            h3(clas.name),
            p(clas.desc)
          )
        )
      }
    )

  def teachers(clas: Clas)(using Lang) =
    div(cls := "clas-teachers")(
      trans.clas.teachersX(
        fragList(clas.teachers.toList.map(t => userIdLink(t.some)))
      )
    )

  def create(form: Form[ClasData])(using PageContext) =
    bits.layout(trans.clas.newClass.txt(), Right("newClass"))(
      cls := "box-pad",
      h1(cls := "box__top")(trans.clas.newClass()),
      innerForm(form, none)
    )

  def edit(c: lila.clas.Clas, students: List[Student.WithUser], form: Form[ClasData])(using PageContext) =
    teacherDashboard.layout(c, students, "edit")(
      div(cls := "box-pad")(
        innerForm(form, c.some),
        hr,
        c.isActive option postForm(
          action := clasRoutes.archive(c.id.value, v = true),
          cls    := "clas-edit__archive"
        )(
          form3.submit(trans.clas.closeClass(), icon = none)(
            cls := "confirm button-red button-empty"
          )
        )
      )
    )

  def notify(c: lila.clas.Clas, students: List[Student.WithUser], form: Form[?])(using PageContext) =
    teacherDashboard.layout(c, students, "wall")(
      div(cls := "box-pad clas-wall__edit")(
        p(
          strong(trans.clas.sendAMessage()),
          br,
          trans.clas.aLinkToTheClassWillBeAdded()
        ),
        postForm(cls := "form3", action := clasRoutes.notifyPost(c.id.value))(
          form3.globalError(form),
          form3.group(
            form("text"),
            frag(trans.message())
          )(form3.textarea(_)(rows := 3)),
          form3.actions(
            a(href := clasRoutes.wall(c.id.value))(trans.cancel()),
            form3.submit(trans.send())
          )
        )
      )
    )

  private def innerForm(form: Form[ClasData], clas: Option[Clas])(using ctx: PageContext) =
    postForm(cls := "form3", action := clas.fold(clasRoutes.create)(c => clasRoutes.update(c.id.value)))(
      form3.globalError(form),
      form3.group(form("name"), trans.clas.className())(form3.input(_)(autofocus)),
      form3.group(
        form("desc"),
        frag(trans.clas.classDescription()),
        help = trans.clas.visibleByBothStudentsAndTeachers().some
      )(form3.textarea(_)(rows := 5)),
      clas match
        case None => form3.hidden(form("teachers"), UserId raw ctx.userId)
        case Some(_) =>
          form3.group(
            form("teachers"),
            trans.clas.teachersOfTheClass(),
            help = trans.clas.addLichessUsernames().some
          )(form3.textarea(_)(rows := 4))
      ,
      form3.actions(
        a(href := clas.fold(clasRoutes.index)(c => clasRoutes.show(c.id.value)))(trans.cancel()),
        form3.submit(trans.apply())
      )
    )
