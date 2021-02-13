package views.html.coach

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.richText

object review {

  import trans.coach._

  def list(reviews: lila.coach.CoachReview.Reviews)(implicit ctx: Context) =
    reviews.list.nonEmpty option div(cls := "coach-show__reviews")(
      h2(studentReviews(reviews.list.size)),
      reviews.list.map { r =>
        div(cls := "coach-review")(
          div(cls := "top")(
            userIdLink(r.userId.some),
            barRating(selected = r.score.some, enabled = false)
          ),
          div(cls := "content")(richText(r.text)),
          isGranted(_.DisapproveCoachReview) option
            postForm(cls := "disapprove", action := routes.Coach.modReview(r.id))(
              submitButton(
                cls := "button button-empty button-red button-thin confirm",
                title := "Instructs the coach to reject the review, or to ask the author to rephrase it."
              )("Disapprove")
            )
        )
      }
    )

  def form(c: lila.coach.Coach.WithUser, mine: Option[lila.coach.CoachReview])(implicit ctx: Context) =
    div(cls := "coach-review-form")(
      if (mine.exists(_.pendingApproval))
        div(cls := "approval")(
          p(thankYouForReview()),
          p(xWillApproveIt(c.user.realNameOrUsername))
        )
      else if (ctx.isAuth) a(cls := "button button-empty toggle")("Write a review")
      else a(href := s"${routes.Auth.login}?referrer=${ctx.req.path}", cls := "button")(reviewCoach()),
      postForm(action := routes.Coach.review(c.user.username))(
        barRating(selected = mine.map(_.score), enabled = true),
        textarea(
          name := "text",
          required,
          minlength := 3,
          maxlength := 2000,
          placeholder := describeExperienceWith.txt(c.user.realNameOrUsername)
        )(mine.map(_.text)),
        submitButton(cls := "button")(trans.apply())
      )
    )

  private val rateStars = tag("rate-stars")
  private val star      = tag("star")

  def barRating(selected: Option[Int], enabled: Boolean) =
    if (enabled)
      select(cls := "rate", name := "score", required)(
        option(value := ""),
        List(1, 2, 3, 4, 5).map { score =>
          option(value := score, selected.contains(score) option st.selected)(score)
        }
      )
    else
      rateStars(
        List(1, 2, 3, 4, 5).map { s =>
          star(selected.exists(s.<=) option (cls := "rate-selected"))
        }
      )
}
