package lila.security

import play.twirl.api.Html

import lila.common.{ Lang, EmailAddress }
import lila.common.String.html.nl2brUnsafe
import lila.user.{ User, UserRepo }

final class AutomaticEmail(
    mailgun: Mailgun,
    baseUrl: String
) {

  def onTitleSet(username: String)(implicit lang: Lang): Funit = for {
    user <- UserRepo named username flatten s"No such user $username"
    emailOption <- UserRepo email user.id
  } yield for {
    title <- user.title
    email <- emailOption
  } yield {

    val profileUrl = s"$baseUrl/@/${user.username}"
    val body = s"""Hello,

Thank you for confirming your $title title on lichess.org.
It is now visible on your profile page: ${baseUrl}/@/${user.username}.

Regards,

The lichess team
"""

    mailgun send Mailgun.Message(
      to = email,
      subject = s"$title title confirmed on lichess.org",
      text = s"""
$body

${Mailgun.txt.serviceNote}
""",
      htmlBody = Html(s"""
<div itemscope itemtype="http://schema.org/EmailMessage">
  <p itemprop="description">${nl2brUnsafe(body)}</p>
  ${Mailgun.html.serviceNote}
</div>""").some
    )
  }

  def onBecomeCoach(user: User)(implicit lang: Lang): Funit =
    UserRepo email user.id flatMap {
      _ ?? { email =>
        val body = s"""Hello,

It is our pleasure to welcome you as a certified lichess coach.
Your coach profile awaits you on ${baseUrl}/coach/edit.

Regards,

The lichess team
"""

        mailgun send Mailgun.Message(
          to = email,
          subject = "Coach profile unlocked on lichess.org",
          text = s"""
$body

${Mailgun.txt.serviceNote}
""",
          htmlBody = Html(s"""
<div itemscope itemtype="http://schema.org/EmailMessage">
  <p itemprop="description">${nl2brUnsafe(body)}</p>
  ${Mailgun.html.serviceNote}
</div>""").some
        )
      }
    }

  def onFishnetKey(userId: User.ID, key: String)(implicit lang: Lang): Funit = for {
    user <- UserRepo named userId flatten s"No such user $userId"
    emailOption <- UserRepo email user.id
  } yield emailOption ?? { email =>

    val body = s"""Hello,

Here is your private fishnet key:

$key


Please treat it like a password. You can use the same key on multiple machines
(even at the same time), but you should not share it with anyone.

Thank you very much for your help! Thanks to you, chess lovers all around the world
will enjoy swift and powerful analysis for their games.

Regards,

The lichess team
"""

    mailgun send Mailgun.Message(
      to = email,
      subject = "Your private fishnet key",
      text = s"""
$body

${Mailgun.txt.serviceNote}
""",
      htmlBody = Html(s"""
<div itemscope itemtype="http://schema.org/EmailMessage">
  <p itemprop="description">${nl2brUnsafe(body)}</p>
  ${Mailgun.html.serviceNote}
</div>""").some
    )
  }
}
