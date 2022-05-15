package views.html

import controllers.routes
import play.api.i18n.Lang
import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.hub.actorApi.timeline._
import lila.poll.Poll
import lila.user.User

object poll {

  def render(poll: Poll, isAuthor: Boolean)(implicit ctx: Context) = {
    div(cls := "poll")( // , action := routes.Poll.vote(poll._id, 1))(
      div(cls := "poll__header")(
        s"${poll.question}",
        form(method := "POST", action := routes.Poll.close(poll._id))(
          submitButton(cls := "button button-empty")(
            "Close"
          )
        )
      ),
      form(method := "POST", cls := "autosubmit", action := routes.Poll.vote(poll._id))(
        st.group(cls := "radio poll__choices")(
          poll.choices.zipWithIndex.map { case (choiceText, i) =>
            val id = s"${poll._id}_$i"
            val prevChoice = poll.votes match {
              case Some(v) => v.getOrElse(ctx.me.get.id, -1)
              case None    => -1
            }
            div(
              input(
                cls   := "choice",
                st.id := id,
                prevChoice == i option st.checked,
                tpe   := "radio",
                value := i,
                name  := "choice"
              ),
              label(`for` := id)(choiceText)
            )
          }.toList
        )
      )
    )
  }

}
