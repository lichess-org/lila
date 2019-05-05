package views.html.coach

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.richText

import controllers.routes

object review {

  def list(c: lila.coach.Coach.WithUser, reviews: lila.coach.CoachReview.Reviews)(implicit ctx: Context) =
    reviews.list.nonEmpty option div(cls := "coach-show__reviews")(
      h2(pluralize("Player review", reviews.list.size)),
      reviews.list.map { r =>
        div(cls := "coach-review")(
          div(cls := "top")(
            userIdLink(r.userId.some),
            barRating(selected = r.score.some, enabled = false)
          ),
          div(cls := "content")(richText(r.text)),
          isGranted(_.DisapproveCoachReview) option
            st.form(cls := "disapprove", method := "post", action := routes.Coach.modReview(r.id))(
              button(cls := "button button-empty button-red button-thin confirm", tpe := "submit", title := "Instructs the coach to reject the review, or to ask the author to rephrase it.")("Disapprove")
            )
        )
      }
    )

  def form(c: lila.coach.Coach.WithUser, mine: Option[lila.coach.CoachReview])(implicit ctx: Context) =
    div(cls := "coach-review-form")(
      if (mine.exists(_.pendingApproval))
        div(cls := "approval")(
        p("Thank you for the review!"),
        p(c.user.realNameOrUsername, " will approve it very soon, or a moderator will have a look at it.")
      )
      else if (ctx.isAuth) a(cls := "button button-empty toggle")("Write a review")
      else a(href := s"${routes.Auth.login}?referrer=${ctx.req.path}", cls := "button")("Review this coach"),
      st.form(action := routes.Coach.review(c.user.username), method := "POST")(
        barRating(selected = mine.map(_.score), enabled = true),
        textarea(
          name := "text",
          required,
          minlength := 3,
          maxlength := 2000,
          placeholder := s"Describe your coaching experience with ${c.user.realNameOrUsername}"
        )(
            mine.map(_.text)
          ),
        button(tpe := "submit", cls := "button")(trans.apply())
      )
    )

  def barRating(selected: Option[Int], enabled: Boolean) =
    if (enabled)
      select(cls := "bar-rating", name := "score", required)(
        option(value := ""),
        List(1, 2, 3, 4, 5).map { score =>
          option(value := score, selected.contains(score) option st.selected)(score)
        }
      )
    else div(cls := "br-wrapper")(
      div(cls := "br-widget br-readonly")(
        List(1, 2, 3, 4, 5).map { s =>
          a(cls := List("br-selected" -> selected.exists(s.<=)))
        }
      )
    )
}
