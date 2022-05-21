package views.html

import controllers.routes
import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.ask.Ask
import lila.security.{ Granter, Permission }

object ask {

  def render(ask: Ask)(implicit ctx: Context) = {
    div(cls := "ask-container")(
      div(cls := "ask", id := ask._id)(
        div(cls := "ask-header")(
          p(cls := "ask-question")(ask.question),
          if (!ask.isConcluded && ctx.me.exists(_.id == ask.creator))
            div(cls := "ask-actions")(
              if (ask.answer.isEmpty)
                button(cls := "button ask-action ask-xhr", formaction := routes.Ask.conclude(ask._id))(
                  "Conclude"
                ),
              button(cls := "button ask-action ask-xhr", formaction := routes.Ask.reset(ask._id))(
                "Reset"
              )
            )
        ),
        if (ask.isConcluded) results(ask)
        else {
          div(cls := "ask-choices")(
            ask.answer match {
              case None      => pollChoices(ask)
              case Some(ans) => quizChoices(ask, ask.choices.indexOf(ans))
            }
          )
        },
        ask.reveal.fold(emptyFrag)(r => if (getPick(ask) > -1) div(cls := "ask-reveal", p(r)))
      )
    )
  }

  private def pollChoices(ask: Ask)(implicit ctx: Context) = frag {
    val isAuthor = ctx.me.exists(_.id == ask.creator)
    val choice   = getPick(ask)
    ask.choices.zipWithIndex.map { case (choiceText, i) =>
      val tooltip = {
        if (ctx.me ?? Granter(Permission.Shusher)) ask.whoPicked(i).mkString(" ")
        else if (isAuthor || ask.isTally) pluralize("vote", ~ask.count(i))
        else choiceText
      }

      val id = s"${ask._id}_$i"
      div(
        title := tooltip,
        button(
          cls        := s"ask-xhr",
          st.id      := id,
          formaction := routes.Ask.pick(ask._id, if (choice != i) i else -1) // let user unset vote
        ),
        label(`for` := id, cls := (if (choice == i) "ask-pick" else ""))(choiceText)
      )
    }.toList
  }

  private def quizChoices(ask: Ask, ans: Int)(implicit ctx: Context) =
    frag {
      val pick      = getPick(ask)
      val hasChoice = pick > -1
      val isAuthor  = ctx.me.exists(_.id == ask.creator)
      ask.choices.zipWithIndex.map { case (choiceText, i) =>
        val sb = new StringBuilder("")
        if ((ask.isTally && hasChoice) || isAuthor || ctx.me ?? Granter(Permission.Shusher))
          sb ++= pluralize("pick", ask.count(i))
        if ((ask.isPublic && hasChoice) || ctx.me ?? Granter(Permission.Shusher))
          sb ++= ask.whoPicked(pick).mkString(if (sb.isEmpty) "" else ": ", " ", "")
        if (sb.isEmpty) sb ++= choiceText
        div(
          title := sb.toString,
          button(
            cls        := (if (hasChoice) "" else "ask-xhr"),
            st.id      := s"${ask._id}_$i",
            formaction := routes.Ask.pick(ask._id, i)
          ),
          label(
            `for` := s"${ask._id}_$i",
            (pick, ans, i) match {
              case x if x._1 == -1   => emptyFrag
              case x if x._2 == x._3 => cls := "ask-correct"
              case x if x._1 == x._3 => cls := "ask-wrong-pick"
              case _                 => cls := "ask-wrong"
            }
          )(choiceText)
        )
      }
    }

  private def results(ask: Ask)(implicit ctx: Context) = {
    div(cls := "ask-bar-graph", id := ask._id)(
      table(
        tbody(frag {
          val countMap = ask.choices.indices
            .map(i => (ask.choices(i), ask.count(i)))
            .toMap
          val countMax = countMap.values.max
          countMap.toSeq.sortBy(_._2)(Ordering.Int.reverse).map { case (choice, count) =>
            val pct = if (countMax == 0) 0 else count * 100 / countMax
            val sb  = new StringBuilder(pluralize("vote", count))
            if (ask.isPublic || ctx.me ?? Granter(Permission.Shusher))
              sb ++= ask.whoPicked(choice).mkString(": ", " ", "")
            tr(
              title := sb.toString,
              td(choice),
              td(div(cls := "pick-bar", css("width") := s"$pct%")(nbsp))
            )
          }
        })
      )
    )
  }

  // convenience method, return -1 if pick not found for ANY reason
  private def getPick(ask: Ask)(implicit ctx: Context): Int =
    ctx.me.fold(-1)(u => ask.picks.fold(-1)(_.getOrElse(u.id, -1)))

  private def pluralize(item: String, n: Int): String =
    if (n == 0) s"No ${item}s" else if (n == 1) s"1 ${item}" else s"$n ${item}s"
}
