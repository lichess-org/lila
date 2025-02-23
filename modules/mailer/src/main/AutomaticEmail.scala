package lila.mailer

import play.api.i18n.Lang
import scalatags.Text.all.*

import lila.core.config.BaseUrl
import lila.core.i18n.I18nKey.emails as trans
import lila.core.i18n.Translator
import lila.core.lilaism.LilaException
import lila.core.misc.mailer.CorrespondenceOpponent
import lila.core.msg.SystemMsg

final class AutomaticEmail(
    userApi: lila.core.user.UserApi,
    mailer: Mailer,
    baseUrl: BaseUrl,
    lightUser: lila.core.user.LightUserApi
)(using Executor, Translator):

  import Mailer.html.*

  val regards = """Regards,

The Lichess team"""

  def welcomeEmail(user: User, email: EmailAddress)(using Lang): Funit =
    lila.mon.email.send.welcome.increment()
    val profileUrl = s"$baseUrl/@/${user.username}"
    val editUrl    = s"$baseUrl/account/profile"
    mailer.send(
      Mailer.Message(
        to = email,
        subject = trans.welcome_subject.txt(user.username),
        text = Mailer.txt.addServiceNote(trans.welcome_text.txt(profileUrl, editUrl)),
        htmlBody = standardEmail(
          trans.welcome_text.txt(profileUrl, editUrl)
        ).some
      )
    )

  def welcomePM(user: User): Funit = fuccess:
    alsoSendAsPrivateMessage(user): lang =>
      given Lang = lang
      import lila.core.i18n.I18nKey
      s"""${I18nKey.onboarding.welcome.txt()}\n${I18nKey.site.lichessPatronInfo.txt()}"""

  def onTitleSet(username: UserStr, title: chess.PlayerTitle): Funit = {
    for
      user        <- userApi.byId(username).orFail(s"No such user $username")
      emailOption <- userApi.email(user.id)
      body = alsoSendAsPrivateMessage(user): _ =>
        s"""Hello,

Thank you for confirming your $title title on Lichess.
It is now visible on your profile page: $baseUrl/@/${user.username}.

$regards
"""
      _ <- emailOption.so { email =>
        given Lang = userLang(user)
        mailer.send(
          Mailer.Message(
            to = email,
            subject = s"$title title confirmed on lichess.org",
            text = Mailer.txt.addServiceNote(body),
            htmlBody = standardEmail(body).some
          )
        )
      }
    yield ()
  }.recover { case e: LilaException =>
    logger.info(e.message)
  }

  def onBecomeCoach(user: User): Funit =
    sendAsPrivateMessageAndEmail(user)(
      subject = _ => "Coach profile unlocked on lichess.org",
      body = _ => s"""Hello,

It is our pleasure to welcome you as a Lichess coach.
Your coach profile awaits you on $baseUrl/coach/edit.

$regards
"""
    )

  def onFishnetKey(userId: UserId, key: String): Funit =
    sendAsPrivateMessageAndEmail(userId)(
      subject = _ => "Your private fishnet key",
      body = _ =>
        s"""Hello,

This message contains your private fishnet key. Please treat it like a password. You can use the same key on multiple machines (even at the same time), but you should not share it with anyone.

Thank you very much for your help! Thanks to you, chess lovers all around the world will enjoy swift and powerful analysis for their games.

Your key is:

$key

$regards
"""
    )

  def onAppealReply(user: User): Funit =
    sendAsPrivateMessageAndEmail(user)(
      subject = _ => "Appeal response on lichess.org",
      body = _ => s"""Hello,

Your appeal has received a response from the moderation team, to see it click here: $baseUrl/appeal

$regards
"""
    )

  def delete(user: User): Funit =
    val body =
      s"""Hello,

Following your request, the Lichess account "${user.username}" will be deleted in 7 days from now.

$regards
"""
    userApi.emailOrPrevious(user.id).flatMapz { email =>
      given Lang = userLang(user)
      mailer.send(
        Mailer.Message(
          to = email,
          subject = "lichess.org account deletion",
          text = Mailer.txt.addServiceNote(body),
          htmlBody = standardEmail(body).some
        )
      )
    }

  def onPatronNew(userId: UserId): Funit =
    userApi
      .byId(userId)
      .map:
        _.foreach: user =>
          alsoSendAsPrivateMessage(user)(
            body = _ =>
              s"""Thank you for supporting Lichess!

Thank you for your donation to Lichess - your patronage directly goes to keeping the site running and new features coming.
Lichess is entirely funded by user's donations like yours, and we truly appreciate the help we're getting.
As a small token of our thanks, your account now has the awesome Patron wings!"""
          )

  def onPatronStop(userId: UserId): Funit =
    userApi
      .byId(userId)
      .map:
        _.foreach: user =>
          alsoSendAsPrivateMessage(user)(
            body = _ =>
              s"""End of Lichess Patron subscription

Thank you for your support over the last month.
We appreciate all donations, being a small team relying entirely on generous donors like you!
If you're still interested in supporting us in other ways, you can see non-financial ways of supporting us here $baseUrl/help/contribute.
To make a new donation, head to $baseUrl/patron"""
          )

  def onPatronGift(from: UserId, to: UserId, lifetime: Boolean): Funit =
    userApi
      .pair(from, to)
      .map:
        _.foreach: (from, to) =>
          val wings =
            if lifetime then "lifetime Patron wings"
            else "Patron wings for one month"
          alsoSendAsPrivateMessage(from): _ =>
            s"""You gifted @${to.username} $wings. Thank you so much!"""
          alsoSendAsPrivateMessage(to): _ =>
            s"""@${from.username} gifted you $wings!"""

  private[mailer] def dailyCorrespondenceNotice(
      userId: UserId,
      opponents: List[CorrespondenceOpponent]
  ): Funit =
    userApi.withEmails(userId).flatMapz { userWithEmail =>
      lightUser.preloadMany(opponents.flatMap(_.opponentId)) >>
        userWithEmail.emails.current
          .filterNot(_.isNoReply)
          .so: email =>
            given Lang = userLang(userWithEmail.user)
            val hello =
              "Hello and thank you for playing correspondence chess on Lichess!"
            val disableSettingNotice =
              "You are receiving this email because you have correspondence email notification turned on. You can turn it off in your settings:"
            val disableLink = s"$baseUrl/account/preferences/notification#correspondence-email-notif"
            mailer.send(
              Mailer.Message(
                to = email,
                subject = "Daily correspondence notice",
                text = Mailer.txt.addServiceNote {
                  s"""$hello

${opponents.map { opponent => s"${showGame(opponent)} $baseUrl/${opponent.gameId}" }.mkString("\n\n")}

$disableSettingNotice $disableLink"""
                },
                htmlBody = emailMessage(
                  p(hello),
                  opponents.map: opponent =>
                    li(
                      showGame(opponent),
                      Mailer.html.url(s"$baseUrl/${opponent.gameId}", clickOrPaste = false)
                    ),
                  disableSettingNotice,
                  Mailer.html.url(disableLink),
                  serviceNote
                ).some
              )
            )
    }

  private def showGame(opponent: CorrespondenceOpponent)(using Lang) =
    val opponentName = opponent.opponentId.fold("Anonymous")(lightUser.syncFallback(_).name)
    opponent.remainingTime.fold(s"It's your turn in your game with $opponentName:"): remainingTime =>
      s"You have ${lila.core.i18n.translateDuration(remainingTime)} remaining in your game with $opponentName:"

  private def alsoSendAsPrivateMessage(user: User)(body: Lang => String): String =
    body(userLang(user)).tap: txt =>
      lila.common.Bus.publish(SystemMsg(user.id, txt), "msgSystemSend")

  private def sendAsPrivateMessageAndEmail(user: User)(subject: Lang => String, body: Lang => String): Funit =
    alsoSendAsPrivateMessage(user)(body).pipe: body =>
      userApi
        .email(user.id)
        .flatMapz: email =>
          given lang: Lang = userLang(user)
          mailer.send(
            Mailer.Message(
              to = email,
              subject = subject(lang),
              text = Mailer.txt.addServiceNote(body),
              htmlBody = standardEmail(body).some
            )
          )

  private def sendAsPrivateMessageAndEmail[U: UserIdOf](
      to: U
  )(subject: Lang => String, body: Lang => String): Funit =
    userApi
      .byId(to)
      .flatMapz: user =>
        sendAsPrivateMessageAndEmail(user)(subject, body)

  private def userLang(user: User): Lang = user.realLang | lila.core.i18n.defaultLang
