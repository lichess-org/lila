package views.html.message

import play.api.data.Form

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.common.paginator.Paginator
import lidraughts.common.String.html.richText

import controllers.routes

object thread {

  def apply(
    thread: lidraughts.message.Thread,
    replyForm: Option[Form[_]],
    blocks: Boolean
  )(implicit ctx: Context, me: lidraughts.user.User) =
    views.html.base.layout(
      title = thread.name,
      moreCss = cssTag("message"),
      moreJs = frag(
        jsTag("message.js"),
        jsAt("compiled/embed-analyse.js")
      )
    ) {
        main(cls := List(
          "message-thread box box-pad page-small" -> true,
          "mod" -> thread.asMod
        ))(
          div(cls := "box__top")(
            h1(
              a(href := routes.Message.inbox(1), dataIcon := "I", cls := "text"),
              thread.nonEmptyName
            ),
            postForm(action := routes.Message.delete(thread.id))(
              button(tpe := "submit", cls := "button button-empty button-red confirm")(trans.delete())
            )
          ),
          thread.posts.map { post =>
            st.article(cls := "message-thread__message embed_analyse", id := s"message_${post.id}")(
              div(cls := "infos")(
                div(
                  userIdLink(thread.visibleSenderOf(post), none),
                  iconTag("H")(cls := "to"),
                  userIdLink(thread.visibleReceiverOf(post), "inline".some)
                ),
                momentFromNow(post.createdAt),
                !thread.isWrittenBy(post, me) option views.html.report.form.flag(
                  username = thread otherUserId me,
                  resource = s"message/${thread.id}",
                  text = post.text
                )
              ),
              div(cls := "message-thread__message__body")(richText(post.text))
            )
          },
          div(cls := "message-thread__answer", id := "bottom")(
            if (blocks)
              p(cls := "end")(
              userIdLink(thread.visibleOtherUserId(me).some),
              " blocks you. You cannot reply."
            )
            else {
              if (!thread.isVisibleByOther(me) && !me.troll) p(cls := "end")(
                userIdLink(thread.visibleOtherUserId(me).some), " has closed this thread. ",
                !thread.asMod option
                  a(
                    href := s"${routes.Message.form}?user=${thread.otherUserId(me)}",
                    cls := "button"
                  )("Create a new one")
              )
              else replyForm.map { form =>
                postForm(action := routes.Message.answer(thread.id))(
                  div(cls := "field_body")(
                    form3.textarea(form("text"))(required),
                    errMsg(form("text"))
                  ),
                  div(cls := "actions")(
                    a(cls := "cancel", href := routes.Message.inbox(1))(trans.cancel()),
                    submitButton(cls := "button text", dataIcon := "E")(trans.send())
                  )
                )
              }
            }
          )
        )
      }
}
