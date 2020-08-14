package views.html
package account

import controllers.routes
import org.joda.time.DateTime

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.user.User

object data {

  def apply(u: lila.user.User, raw: String)(implicit
      ctx: Context
  ) =
    account.layout(title = s"${u.username} - personal data", active = "security") {
      div(cls := "account security personal-data box box-pad")(
        h1("My personal data"),
        div(cls := "personal-data__header")(
          p("Here is all personal information Lichess has about ", userLink(u)),
          a(cls := "button", href := s"${routes.Account.data()}?text=1")(trans.downloadRaw())
        ),
        st.section(
          pre(cls := "raw-text")(raw)
        )
      )
    }

  def rawText(
      u: lila.user.User,
      sessions: List[lila.security.UserSession],
      posts: List[lila.forum.Post],
      msgs: Seq[(User.ID, String, DateTime)]
  ) =
    List(
      privateMessages(msgs),
      forumPosts(posts),
      connections(sessions)
    ).flatten mkString "\n\n"

  private def connections(sessions: List[lila.security.UserSession]) =
    List(
      textTitle(s"${sessions.size} Connections"),
      sessions.map { s =>
        s"${s.ip} ${s.date.map(showEnglishDateTime)}\n${s.ua}"
      } mkString "\n\n"
    )

  private def forumPosts(posts: List[lila.forum.Post]) =
    List(
      textTitle(s"${posts.size} Forum posts"),
      posts.map { p =>
        s"${showEnglishDateTime(p.createdAt)}\n${p.text}"
      } mkString "\n\n------------------------------------------\n\n"
    )

  private def privateMessages(msgs: Seq[(User.ID, String, DateTime)]) =
    List(
      textTitle(s"${msgs.size} Direct messages"),
      msgs.map {
        case (to, text, date) =>
          s"$to ${showEnglishDateTime(date)}\n$text"
      } mkString "\n\n------------------------------------------\n\n"
    )

  private def textTitle(t: String) = s"\n\n${"=" * t.size}\n$t\n${"=" * t.size}\n\n\n"
}
