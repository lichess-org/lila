package lila.clas
package ui

import play.api.data.Form

import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class ClasPages(helpers: Helpers, clasUi: ClasUi, dashUi: DashboardUi):
  import helpers.{ *, given }
  import clasUi.ClasPage

  def home(using Context) =
    Page(trans.clas.lichessClasses.txt())
      .css("bits.page", "bits.clas"):
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

  def teacherIndex(classes: List[Clas], closed: Boolean)(using Context) =
    val (active, archived) = classes.partition(_.isActive)
    val (current, others) = if closed then (archived, active) else (active, archived)
    ClasPage(trans.clas.lichessClasses.txt(), Right("classes"))(cls := "clas-index"):
      frag(
        div(cls := "box__top")(
          h1(cls := "box__top")(trans.clas.lichessClasses()),
          a(
            href := routes.Clas.form,
            cls := "new button button-empty",
            title := trans.clas.newClass.txt(),
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

  def studentIndex(classes: List[Clas])(using Context) =
    ClasPage(trans.clas.lichessClasses.txt(), Right("classes"))(cls := "clas-index"):
      frag(
        boxTop(h1(trans.clas.lichessClasses())),
        renderClasses(classes)
      )

  private def renderClasses(classes: List[Clas]) =
    div(cls := "classes")(
      classes.map { clas =>
        div(
          cls := List("clas-widget" -> true, "clas-widget-archived" -> clas.isArchived),
          dataIcon := Icon.Group
        )(
          a(cls := "overlay", href := routes.Clas.show(clas.id)),
          div(
            h3(clas.name),
            p(clas.desc)
          )
        )
      }
    )

  def create(form: lila.core.security.HcaptchaForm[ClasForm.ClasData])(using Context) =
    ClasPage(trans.clas.newClass.txt(), Right("newClass"))(cls := "box-pad")
      .js(hcaptchaScript(form))
      .csp(_.withHcaptcha):
        frag(
          h1(cls := "box__top")(trans.clas.newClass()),
          postForm(cls := "form3", action := routes.Clas.create)(
            clasForm(form.form, none),
            lila.ui.bits.hcaptcha(form),
            form3.actions(
              a(href := routes.Clas.index)(trans.site.cancel()),
              form3.submit(trans.site.apply())
            )
          )
        )

  def edit(c: lila.clas.Clas, students: List[Student.WithUser], form: Form[ClasForm.ClasData])(using
      Context
  ) =
    dashUi.teacher.TeacherPage(c, students, "edit")()(
      div(cls := "box-pad")(
        postForm(cls := "form3", action := routes.Clas.update(c.id))(
          clasForm(form, c.some),
          form3.actions(
            a(href := routes.Clas.show(c.id))(trans.site.cancel()),
            form3.submit(trans.site.apply())
          )
        ),
        hr,
        c.isActive.option(
          postForm(
            action := routes.Clas.archive(c.id, v = true),
            cls := "clas-edit__archive"
          )(
            form3.submit(trans.clas.closeClass(), icon = none)(
              cls := "yes-no-confirm button-red button-empty"
            )
          )
        )
      )
    )

  private def clasForm(form: Form[ClasForm.ClasData], clas: Option[Clas])(using ctx: Context) =
    frag(
      form3.globalError(form),
      form3.group(form("name"), trans.clas.className())(form3.input(_)(autofocus)),
      form3.group(
        form("desc"),
        frag(trans.clas.classDescription()),
        help = trans.clas.visibleByBothStudentsAndTeachers().some
      )(form3.textarea(_)(rows := 5)),
      clas match
        case None => form3.hidden(form("teachers"), UserId.raw(ctx.userId))
        case Some(_) =>
          form3.group(
            form("teachers"),
            trans.clas.teachersOfTheClass(),
            help = trans.clas.addLichessUsernames().some
          )(form3.textarea(_)(rows := 4))
    )
