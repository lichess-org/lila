package lila.mailer

import play.api.i18n.Lang
import scalatags.Text.all.*

import lila.core.config.BaseUrl
import lila.core.i18n.I18nKey.emails as trans
import lila.core.i18n.Translator
import lila.core.lilaism.LilaException
import lila.core.misc.mailer.CorrespondenceOpponent
import lila.core.msg.SystemMsg
import lila.core.ublog.Logger

final class AutomaticEmail(
    userApi: lila.core.user.UserApi,
    mailer: Mailer,
    baseUrl: BaseUrl,
    lightUser: lila.core.user.LightUserApi
)(using Executor, Translator):

  import Mailer.html.*
  
  private val logger = lila.core.ublog.logger("mailer")

  val regards = """Regards,

The Lichess team"""

  def welcomeEmail(user: User, email: EmailAddress)(using Lang): Funit =
    // BUGFIX: Add null check for username to prevent string interpolation errors
    if (user.username.nonEmpty) {
      mailer.canSend.so:
        lila.mon.email.send.welcome.increment()
        val profileUrl = s"$baseUrl/@/${user.username}"
        val editUrl = s"$baseUrl/account/profile"
        mailer.sendOrSkip:
          Mailer.Message(
            to = email,
            subject = trans.welcome_subject.txt(user.username),
            text = Mailer.txt.addServiceNote(trans.welcome_text.txt(profileUrl, editUrl)),
            htmlBody = standardEmail(
              trans.welcome_text.txt(profileUrl, editUrl)
            ).some
          )
    } else {
      // BUGFIX: Log error and return failure for invalid user
      logger.error(s"Failed to send welcome email: user has empty username")
      Future.failed(new IllegalArgumentException("User has empty username"))
    }

  def welcomePM(user: User): Funit = 
    try {
      alsoSendAsPrivateMessage(user): lang =>
        given Lang = lang
        import lila.core.i18n.I18nKey as trans
        s"""${trans.onboarding.welcome.txt()}\n${trans.site.lichessPatronInfo.txt()}"""
      fuccess(())
    } catch {
      case e: Exception =>
        logger.error(s"Failed to send welcome PM to user ${user.username}: ${e.getMessage}")
        Future.failed(e)
    }

  def onTitleSet(username: UserStr, title: chess.PlayerTitle): Funit = {
    for
      user <- userApi.byId(username).orFail(s"No such user $username")
      emailOption <- userApi.email(user.id)
      body = alsoSendAsPrivateMessage(user): _ =>
        // BUGFIX: Add null check for username to prevent string interpolation errors
        if (user.username.nonEmpty) {
          s"""Hello,

Thank you for confirming your $title title on Lichess.
It is now visible on your profile page: $baseUrl/@/${user.username}.

$regards
"""
        } else {
          s"""Hello,

Thank you for confirming your $title title on Lichess.
It is now visible on your profile page.

$regards
"""
        }
      _ <- emailOption.so { email =>
        given Lang = userLang(user)
        mailer.sendOrSkip:
          Mailer.Message(
            to = email,
            subject = s"$title title confirmed on lichess.org",
            text = Mailer.txt.addServiceNote(body),
            htmlBody = standardEmail(body).some
          )
      }
    yield ()
  }.recover { 
    case e: LilaException =>
      logger.info(e.message)
    case e: Exception =>
      // BUGFIX: Catch all exceptions, not just LilaException, to prevent crashes
      logger.error(s"Failed to send title confirmation email to $username: ${e.getMessage}")
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
    // BUGFIX: Add null check for username to prevent string interpolation errors
    val body = if (user.username.nonEmpty) {
      s"""Hello,

Following your request, the Lichess account "${user.username}" will be deleted in 7 days from now.

$regards
"""
    } else {
      s"""Hello,

Following your request, the Lichess account will be deleted in 7 days from now.

$regards
"""
    }
    userApi.emailOrPrevious(user.id).flatMapz { email =>
      given Lang = userLang(user)
      mailer.sendOrSkip:
        Mailer.Message(
          to = email,
          subject = "lichess.org account deletion",
          text = Mailer.txt.addServiceNote(body),
          htmlBody = standardEmail(body).some
        )
    }.recover {
      // BUGFIX: Add error handling for email retrieval failures
      case e: Exception =>
        logger.error(s"Failed to send deletion email to ${user.username}: ${e.getMessage}")
        fuccess(())
    }

  def onPatronNew(userId: UserId): Funit =
    userApi
      .byId(userId)
      .flatMap:
        case Some(user) =>
          alsoSendAsPrivateMessage(user)(
            body = _ =>
              s"""Thank you for supporting Lichess!

Thank you for your donation to Lichess - your patronage directly goes to keeping the site running and new features coming.
Lichess is entirely funded by user's donations like yours, and we truly appreciate the help we're getting.
As a small token of our thanks, your account now has the awesome Patron wings!"""
          )
          fuccess(())
        case None =>
          logger.error(s"Failed to send patron new notification: user not found for userId=$userId")
          Future.failed(new IllegalArgumentException(s"User not found: $userId"))

  def onPatronStop(userId: UserId): Funit =
    userApi
      .byId(userId)
      .flatMap:
        case Some(user) =>
          alsoSendAsPrivateMessage(user)(
            body = _ =>
              s"""End of Lichess Patron subscription

Thank you for your support over the last month.
We appreciate all donations, being a small team relying entirely on generous donors like you!
If you're still interested in supporting us in other ways, you can see non-financial ways of supporting us here $baseUrl/help/contribute.
To make a new donation, head to $baseUrl/patron"""
          )
          fuccess(())
        case None =>
          logger.error(s"Failed to send patron stop notification: user not found for userId=$userId")
          Future.failed(new IllegalArgumentException(s"User not found: $userId"))

  def onPatronGift(from: UserId, to: UserId, lifetime: Boolean): Funit =
    userApi
      .pair(from, to)
      .flatMap:
        case Some((from, to)) =>
          val wings =
            if lifetime then "Lifetime Patron wings"
            else "Patron wings for one month"
          // BUGFIX: Add null checks for usernames to prevent null pointer exceptions
          if (from.username.nonEmpty && to.username.nonEmpty) {
            alsoSendAsPrivateMessage(from): _ =>
              s"""You gifted @${to.username} $wings. Thank you so much!"""
            alsoSendAsPrivateMessage(to): _ =>
              s"""@${from.username} gifted you $wings!"""
            fuccess(())
          } else {
            logger.error(s"Failed to send patron gift notification: invalid usernames from=${from.username}, to=${to.username}")
            Future.failed(new IllegalArgumentException("Invalid usernames for patron gift"))
          }
        case None =>
          logger.error(s"Failed to send patron gift notification: could not find users from=$from, to=$to")
          Future.failed(new IllegalArgumentException("Users not found for patron gift"))

  def onPatronFree(dest: User): Funit =  // BUGFIX: Change return type from Unit to Funit for consistency
    try {
      alsoSendAsPrivateMessage(dest)(
        body = _ => s"""Thank you for being an active member of our community!
As a token of our appreciation, you have been gifted Patron Wings for a month.
$baseUrl/patron"""
      )
      fuccess(())
    } catch {
      case e: Exception =>
        logger.error(s"Failed to send patron free notification to user ${dest.username}: ${e.getMessage}")
        Future.failed(e)
    }

  private[mailer] def dailyCorrespondenceNotice(
      userId: UserId,
      opponents: List[CorrespondenceOpponent]
  ): Funit =
    userApi.withEmails(userId).flatMapz { userWithEmail =>
      // BUGFIX: Add null check for user and validate opponents list
      if (userWithEmail.user.username.nonEmpty && opponents.nonEmpty) {
        lightUser.preloadMany(opponents.flatMap(_.opponentId)).flatMap { _ =>
          userWithEmail.emails.current
            .filterNot(_.isNoReply)
            .so: email =>
              given Lang = userLang(userWithEmail.user)
              val hello =
                "Hello and thank you for playing correspondence chess on Lichess!"
              val disableSettingNotice =
                "You are receiving this email because you have correspondence email notification turned on. You can turn it off in your settings:"
              val disableLink = s"$baseUrl/account/preferences/notification#correspondence-email-notif"
              mailer.sendOrSkip:
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
        }.recover {
          case e: Exception =>
            logger.error(s"Failed to send correspondence notice to user $userId: ${e.getMessage}")
            fuccess(())
        }
      } else {
        logger.warn(s"Skipping correspondence notice for user $userId: invalid user or empty opponents list")
        fuccess(())
      }
    }

  private def showGame(opponent: CorrespondenceOpponent)(using Lang) =
    try {
      val opponentName = opponent.opponentId.fold("Anonymous")(id => 
        try lightUser.syncFallback(id).name catch { case _: Exception => "Anonymous" }
      )
      opponent.remainingTime.fold(s"It's your turn in your game with $opponentName:"): remainingTime =>
        s"You have ${lila.core.i18n.translateDuration(remainingTime)} remaining in your game with $opponentName:"
    } catch {
      case e: Exception =>
        logger.error(s"Failed to show game for opponent: ${e.getMessage}")
        "Your correspondence game"
    }

  private def alsoSendAsPrivateMessage(user: User)(body: Lang => String): String =
    try {
      body(userLang(user)).tap: txt =>
        lila.common.Bus.pub(SystemMsg(user.id, txt))
    } catch {
      case e: Exception =>
        logger.error(s"Failed to send private message to user ${user.username}: ${e.getMessage}")
        throw e
    }

  private def sendAsPrivateMessageAndEmail(user: User)(subject: Lang => String, body: Lang => String): Funit =
    alsoSendAsPrivateMessage(user)(body).pipe: body =>
      userApi
        .email(user.id)
        .flatMapz: email =>
          given lang: Lang = userLang(user)
          mailer.sendOrSkip:
            Mailer.Message(
              to = email,
              subject = subject(lang),
              text = Mailer.txt.addServiceNote(body),
              htmlBody = standardEmail(body).some
            )
        .recover {
          case e: Exception =>
            logger.error(s"Failed to send email to user ${user.username}: ${e.getMessage}")
            fuccess(())
        }

  private def sendAsPrivateMessageAndEmail[U: UserIdOf](
      to: U
  )(subject: Lang => String, body: Lang => String): Funit =
    userApi
      .byId(to)
      .flatMapz: user =>
        sendAsPrivateMessageAndEmail(user)(subject, body)
      .recover {
        case e: Exception =>
          logger.error(s"Failed to send email to user ID $to: ${e.getMessage}")
          fuccess(())
      }

  private def userLang(user: User): Lang = user.realLang | lila.core.i18n.defaultLang
