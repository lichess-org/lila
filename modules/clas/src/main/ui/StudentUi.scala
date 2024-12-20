package lila.clas
package ui

import scalalib.model.Days
import lila.core.config.NetDomain
import lila.ui.*
import lila.ui.ScalatagsTemplate.{ *, given }

final class StudentUi(helpers: Helpers, clasUi: ClasUi)(using NetDomain):
  import helpers.{ *, given }
  import clasUi.ClasPage

  def show(clas: Clas, students: List[Student], s: Student.WithUserAndManagingClas, activities: Frag)(using
      ctx: Context
  ) =
    ClasPage(s.user.username.value, Left(clas.withStudents(students)), s.student.some)(cls := "student-show"):
      frag(
        top(clas, s.withUser),
        div(cls := "box__pad")(
          standardFlash,
          ctx.flash("password").map { password =>
            flashMessageWith(cls := "student-show__password")(
              div(
                p(trans.clas.makeSureToCopy()),
                pre(trans.clas.passwordX(password))
              )
            )
          },
          s.student.archived.map: archived =>
            div(cls := "student-show__archived archived")(
              clasUi.showArchived(archived),
              div(cls := "student-show__archived__actions")(
                postForm(action := routes.Clas.studentArchive(clas.id, s.user.username, v = false)):
                  form3.submit(trans.clas.inviteTheStudentBack(), icon = none)(
                    cls := "yes-no-confirm button-empty"
                  )
                ,
                postForm(action := routes.Clas.studentClosePost(clas.id, s.user.username)):
                  form3.submit(trans.clas.removeStudent(), icon = none)(
                    cls   := "yes-no-confirm button-red button-empty",
                    title := "Fully erase the student from the class archives."
                  )
              )
            ),
          s.student.notes.nonEmpty.option:
            div(cls := "student-show__notes")(richText(s.student.notes, nl2br = true, expandImg = false))
          ,
          s.student.managed
            .option(
              div(cls := "student-show__managed")(
                p(trans.clas.thisStudentAccountIsManaged()),
                div(cls := "student-show__managed__actions")(
                  postForm(action := routes.Clas.studentResetPassword(clas.id, s.user.username))(
                    form3.submit(trans.clas.resetPassword(), icon = none)(
                      s.student.isArchived.option(disabled),
                      cls := List(
                        "yes-no-confirm button button-empty" -> true,
                        "disabled"                           -> s.student.isArchived
                      ),
                      title := trans.clas.generateANewPassword.txt()
                    )
                  ),
                  a(
                    href  := routes.Clas.studentRelease(clas.id, s.user.username),
                    cls   := "button button-empty",
                    title := trans.clas.upgradeFromManaged.txt()
                  )(trans.clas.release())
                )
              )
            )
            .orElse(s.managingClas.map { managingClas =>
              div(cls := "student-show__managed")(
                p(trans.clas.thisStudentAccountIsManaged()),
                a(href := routes.Clas.studentShow(managingClas.id, s.user.username))(
                  "Class: ",
                  managingClas.name
                )
              )
            }),
          activities
        )
      )

  def top(clas: Clas, s: Student.WithUserLike)(using Context) =
    div(cls := "student-show__top")(
      boxTop(
        h1(dataIcon := Icon.User)(
          span(
            strong(s.user.username),
            em(s.student.realName)
          )
        )
      ),
      div(cls := "student-show__top__meta")(
        p(
          trans.clas.invitedToXByY(
            a(href := routes.Clas.show(clas.id))(clas.name),
            userIdLink(s.student.created.by.some, withOnline = false)
          ),
          " ",
          momentFromNowOnce(s.student.created.at)
        ),
        div(
          a(
            href := routes.Msg.convo(s.user.username),
            cls  := "button button-empty"
          )(trans.site.message()),
          a(
            href := routes.Clas.studentEdit(clas.id, s.user.username),
            cls  := "button button-empty"
          )(trans.site.edit()),
          a(
            href := routes.User.show(s.user.username),
            cls  := "button button-empty"
          )(trans.site.profile()),
          a(
            href := routes.Puzzle.dashboard(Days(7), "home", s.user.username.some),
            cls  := "button button-empty"
          )(trans.puzzle.puzzleDashboard()),
          Granter
            .opt(_.Beta)
            .option(
              a(
                href := routes.Tutor.user(s.user.username),
                cls  := "button button-empty"
              )("Tutor")
            )
        )
      )
    )

  def invite(c: Clas, invite: ClasInvite)(using Context) =
    Page(c.name).css("bits.clas"):
      main(cls := "page-small box box-pad page clas-invitation")(
        h1(cls := "box__top")(c.name),
        p(c.desc),
        br,
        br,
        p(trans.clas.youHaveBeenInvitedByX(userIdLink(invite.created.by.some))),
        br,
        br,
        invite.accepted.map {
          if _ then flashMessage("success")(trans.clas.youAcceptedThisInvitation())
          else flashMessage("warning")(trans.clas.youDeclinedThisInvitation())
        },
        invite.accepted
          .forall(false.==)
          .option(
            postForm(cls := "form3", action := routes.Clas.invitationAccept(invite.id))(
              form3.actions(
                if !invite.accepted.has(false) then
                  form3.submit(
                    trans.site.decline(),
                    nameValue = ("v" -> false.toString).some,
                    icon = Icon.X.some
                  )(cls := "button-red button-fat")
                else p,
                form3.submit(
                  trans.site.accept(),
                  nameValue = ("v" -> true.toString).some
                )(cls := "button-green button-fat")
              )
            )
          )
      )
