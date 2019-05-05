package lila.security

import scalatags.Text.all._

import lila.common.{ Lang, EmailAddress }
import lila.i18n.I18nKeys.{ emails => trans }
import lila.user.{ User, UserRepo }

final class AutomaticEmail(
    mailgun: Mailgun,
    baseUrl: String
) {

  import Mailgun.html._

  def welcome(user: User, email: EmailAddress)(implicit lang: Lang): Funit = {
    val profileUrl = s"$baseUrl/@/${user.username}"
    val editUrl = s"$baseUrl/account/profile"
    mailgun send Mailgun.Message(
      to = email,
      subject = trans.welcome_subject.literalTxtTo(lang, List(user.username)),
      text = s"""
${trans.welcome_text.literalTxtTo(lang, List(profileUrl, editUrl))}

${Mailgun.txt.serviceNote}
""",
      htmlBody = standardEmail(
        trans.welcome_text.literalTxtTo(lang, List(profileUrl, editUrl))
      ).some
    )
  }

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
      htmlBody = standardEmail(body).some
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
          htmlBody = standardEmail(body).some
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
      htmlBody = standardEmail(body).some
    )
  }
}
