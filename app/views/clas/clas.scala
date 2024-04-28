package views.clas
package clas

import play.api.data.Form
import play.api.i18n.Lang

import lila.app.templating.Environment.{ *, given }

import lila.clas.ClasForm.ClasData
import lila.clas.{ Clas, Student }

def home(using PageContext) =
  views.base.layout(
    moreCss = frag(
      cssTag("page"),
      cssTag("clas")
    ),
    title = trans.clas.lichessClasses.txt()
  ):
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
        postForm(action := routes.Clas.becomeTeacher)(
          submitButton(cls := "button button-fat")(trans.clas.applyToBeLichessTeacher())
        )
      )
    )

def teacherIndex(classes: List[Clas], closed: Boolean)(using PageContext) =
  val (active, archived) = classes.partition(_.isActive)
  val (current, others)  = if closed then (archived, active) else (active, archived)
  layout(trans.clas.lichessClasses.txt(), Right("classes"))(
    cls := "clas-index",
    div(cls := "box__top")(
      h1(cls := "box__top")(trans.clas.lichessClasses()),
      a(
        href     := routes.Clas.form,
        cls      := "new button button-empty",
        title    := trans.clas.newClass.txt(),
        dataIcon := Icon.PlusButton
      )
    ),
    if current.isEmpty then frag(hr, p(cls := "box__pad classes__empty")(trans.clas.noClassesYet()))
    else renderClasses(current),
    (closed || others.nonEmpty).option(
      div(cls := "clas-index__others")(
        a(href := s"${routes.Clas.index}?closed=${!closed}")(
          others.size.localize,
          " ",
          if closed then "active" else "archived",
          if others.size == 1 then " class" else " classes"
        )
      )
    )
  )

def studentIndex(classes: List[Clas])(using PageContext) =
  layout(trans.clas.lichessClasses.txt(), Right("classes"))(
    cls := "clas-index",
    boxTop(h1(trans.clas.lichessClasses())),
    renderClasses(classes)
  )

private def renderClasses(classes: List[Clas]) =
  div(cls := "classes")(
    classes.map { clas =>
      div(
        cls      := List("clas-widget" -> true, "clas-widget-archived" -> clas.isArchived),
        dataIcon := Icon.Group
      )(
        a(cls := "overlay", href := routes.Clas.show(clas.id.value)),
        div(
          h3(clas.name),
          p(clas.desc)
        )
      )
    }
  )

def create(form: lila.core.security.HcaptchaForm[ClasData])(using PageContext) =
  layout(
    trans.clas.newClass.txt(),
    Right("newClass"),
    moreJs = lila.web.views.hcaptcha.script(form),
    csp = defaultCsp.withHcaptcha.some
  )(
    cls := "box-pad",
    h1(cls := "box__top")(trans.clas.newClass()),
    postForm(cls := "form3", action := routes.Clas.create)(
      ui.clasForm(form.form, none),
      lila.ui.bits.hcaptcha(form),
      form3.actions(
        a(href := routes.Clas.index)(trans.site.cancel()),
        form3.submit(trans.site.apply())
      )
    )
  )

def edit(c: lila.clas.Clas, students: List[Student.WithUser], form: Form[ClasData])(using PageContext) =
  teacherDashboard.layout(c, students, "edit")(
    div(cls := "box-pad")(
      postForm(cls := "form3", action := routes.Clas.update(c.id.value))(
        ui.clasForm(form, c.some),
        form3.actions(
          a(href := routes.Clas.show(c.id.value))(trans.site.cancel()),
          form3.submit(trans.site.apply())
        )
      ),
      hr,
      c.isActive.option(
        postForm(
          action := routes.Clas.archive(c.id.value, v = true),
          cls    := "clas-edit__archive"
        )(
          form3.submit(trans.clas.closeClass(), icon = none)(
            cls := "confirm button-red button-empty"
          )
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
      postForm(cls := "form3", action := routes.Clas.notifyPost(c.id.value))(
        form3.globalError(form),
        form3.group(
          form("text"),
          frag(trans.site.message())
        )(form3.textarea(_)(rows := 3)),
        form3.actions(
          a(href := routes.Clas.wall(c.id.value))(trans.site.cancel()),
          form3.submit(trans.site.send())
        )
      )
    )
  )
