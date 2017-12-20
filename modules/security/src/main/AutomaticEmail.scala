package lila.security

import play.api.i18n.Lang

import lila.common.EmailAddress
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
    val body = s"""
Hello,

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
      htmlBody = s"""
<div itemscope itemtype="http://schema.org/EmailMessage">
  <p itemprop="description">$body</p>
  ${Mailgun.html.serviceNote}
</div>""".some
    )
  }

  def onBecomeCoach(user: User)(implicit lang: Lang): Funit =
    UserRepo email user.id flatMap {
      _ ?? { email =>
        val body = s"""
Hello,

It is our pleasure to welcome you as a certified lichess coach.
Your coach profile awaits you on ${baseUrl}/@/coach/edit.

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
          htmlBody = s"""
<div itemscope itemtype="http://schema.org/EmailMessage">
  <p itemprop="description">$body</p>
  ${Mailgun.html.serviceNote}
</div>""".some
        )
      }
    }
}
