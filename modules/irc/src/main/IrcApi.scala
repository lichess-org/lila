package lila.irc

import org.joda.time.DateTime

import lila.common.IpAddress
import lila.common.{ ApiVersion, EmailAddress, Heapsort, IpAddress, LightUser }
import lila.hub.actorApi.irc._
import lila.user.Holder
import lila.user.User

final class IrcApi(
    zulip: ZulipClient,
    noteApi: lila.user.NoteApi,
    implicit val lightUser: LightUser.Getter
)(implicit ec: scala.concurrent.ExecutionContext) {

  import IrcApi._

  def commReportBurst(user: User): Funit = {
    val md = markdown.linkifyUsers(s"Burst of comm reports about @${user.username}")
    zulip(_.mod.commsPrivate, "burst")(md)
  }

  def inquiry(user: User, mod: Holder, domain: ModDomain, room: String): Funit = {
    val stream = domain match {
      case ModDomain.Comm  => ZulipClient.stream.mod.commsPrivate
      case ModDomain.Cheat => ZulipClient.stream.mod.hunterCheat
      case ModDomain.Boost => ZulipClient.stream.mod.hunterBoost
      case _               => ZulipClient.stream.mod.adminGeneral
    }
    noteApi
      .byUserForMod(user.id)
      .map(_.headOption.filter(_.date isAfter DateTime.now.minusMinutes(5)))
      .flatMap {
        case None =>
          zulip.sendAndGetLink(stream, "/" + user.username)(
            s"${markdown.userLink(mod.user.username)} :monkahmm: is looking at a $room report about **${markdown
                .userLink(user.username)}**"
          )
        case Some(note) =>
          zulip.sendAndGetLink(stream, "/" + user.username)(
            s"${markdown.modLink(mod.user)} :pepenote: **${markdown
                .userLink(user.username)}** (${markdown.userNotesLink(user.username)}):\n" +
              markdown.linkifyUsers(note.text take 2000)
          )
      }
      .flatMap {
        _ ?? { ZulipLink =>
          noteApi.write(
            user,
            s"$domain discussion: $ZulipLink",
            mod.user,
            modOnly = true,
            dox = domain == ModDomain.Admin
          )
        }
      }
  }

  def userModNote(modName: String, username: String, note: String): Funit =
    !User.isLichess(modName) ??
      zulip(_.mod.adminLog, "notes")(
        s"${markdown.modLink(modName)} :note: **${markdown.userLink(username)}** (${markdown.userNotesLink(username)}):\n" +
          markdown.linkifyUsers(note take 2000)
      )

  def selfReport(typ: String, path: String, user: User, ip: IpAddress): Funit =
    zulip(_.mod.adminLog, "self report")(
      s"[**$typ**] ${markdown.userLink(user)}@$ip ${markdown.gameLink(path)}"
    )

  def commlog(mod: Holder, user: User, reportBy: Option[User.ID]): Funit =
    zulip(_.mod.adminLog, "private comms checks")({
      val finalS = if (user.username endsWith "s") "" else "s"
      s"**${markdown modLink mod.user}** checked out **${markdown userLink user.username}**'$finalS communications "
    } + reportBy.filter(mod.id !=).fold("spontaneously") { by =>
      s"while investigating a report created by ${markdown.userLink(by)}"
    })

  def monitorMod(modId: User.ID, icon: String, text: String, tpe: ModDomain): Funit =
    lightUser(modId) flatMap {
      _ ?? { mod =>
        zulip(_.mod.adminMonitor(tpe), mod.name)(
          s"${markdown.userLink(mod.name)} :$icon: ${markdown.linkifyPostsAndUsers(text)}"
        )
      }
    }

  def logMod(modId: User.ID, icon: String, text: String): Funit =
    lightUser(modId) flatMap {
      _ ?? { mod =>
        zulip(_.mod.log, "actions")(
          s"${markdown.modLink(modId)} :$icon: ${markdown.linkifyPostsAndUsers(text)}"
        )
      }
    }

  // def printBan(mod: Holder, print: String, userIds: List[User.ID]): Funit =
  //   logMod(mod.id, "footprints", s"Ban print $print of ${userIds} users: ${userIds map linkifyUsers}")

  def chatPanic(mod: Holder, v: Boolean): Funit = {
    val msg =
      s":stop: ${markdown.modLink(mod.user)} ${if (v) "enabled" else "disabled"} ${markdown.lichessLink("/mod/chat-panic", " Chat Panic")}"
    zulip(_.mod.log, "chat panic")(msg) >> zulip(_.mod.commsPublic, "main")(msg)
  }

  def garbageCollector(msg: String): Funit =
    zulip(_.mod.adminLog, "garbage collector")(markdown linkifyUsers msg)

  def broadcastError(id: String, name: String, error: String): Funit =
    zulip(_.broadcast, "lila error log")(s"${markdown.broadcastLink(id, name)} $error")

  def ublogPost(
      user: User,
      id: String,
      slug: String,
      title: String,
      intro: String
  ): Funit =
    zulip(_.blog, "non-tiered new posts")(
      s":note: ${markdown
          .lichessLink(s"/@/${user.username}/blog/$slug/$id", title)} $intro - by ${markdown
          .userLink(user)}"
    )

  def userAppeal(user: User, mod: Holder): Funit =
    zulip
      .sendAndGetLink(_.mod.adminAppeal, "/" + user.username)(
        s"${markdown.modLink(mod.user)} :monkahmm: is looking at the appeal of **${markdown
            .lichessLink(s"/appeal/${user.username}", user.username)}**"
      )
      .flatMap {
        _ ?? { zulipAppealConv =>
          noteApi.write(user, s"Appeal discussion: $zulipAppealConv", mod.user, modOnly = true, dox = true)
        }
      }

  def nameClosePreset(username: String): Funit =
    zulip(_.mod.usernames, "/" + username)("@**remind** here in 48h to close this account")

  def stop(): Funit = zulip(_.general, "lila")("Lichess is restarting.")

  def publishEvent(event: Event): Funit = event match {
    case Error(msg)   => publishError(msg)
    case Warning(msg) => publishWarning(msg)
    case Info(msg)    => publishInfo(msg)
    case Victory(msg) => publishVictory(msg)
  }

  def signupAfterTryingDisposableEmail(user: User, email: EmailAddress, previous: Set[EmailAddress]) =
    zulip(_.mod.adminLog, "disposable email")(
      s"${markdown userLink user} signed up with ${email.value} after trying: ${previous.map(_.value) mkString ", "}"
    )

  private def publishError(msg: String): Funit =
    zulip(_.general, "lila")(s":lightning: ${markdown linkifyUsers msg}")

  private def publishWarning(msg: String): Funit =
    zulip(_.general, "lila")(s":thinking: ${markdown linkifyUsers msg}")

  private def publishVictory(msg: String): Funit =
    zulip(_.general, "lila")(s":tada: ${markdown linkifyUsers msg}")

  private[irc] def publishInfo(msg: String): Funit =
    zulip(_.general, "lila")(s":info: ${markdown linkifyUsers msg}")

  object charge {
    import lila.hub.actorApi.plan.ChargeEvent
    private var buffer: Vector[ChargeEvent] = Vector.empty
    implicit private val amountOrdering     = Ordering.by[ChargeEvent, Int](_.cents)

    def apply(event: ChargeEvent): Funit = {
      buffer = buffer :+ event
      buffer.head.date.isBefore(DateTime.now.minusHours(12)) ?? {
        val firsts    = Heapsort.topN(buffer, 10, amountOrdering).map(_.username).map(userAt).mkString(", ")
        val amountSum = buffer.map(_.cents).sum
        val patrons =
          if (firsts.lengthIs > 10) s"$firsts and, like, ${firsts.length - 10} others,"
          else firsts
        displayMessage {
          s"$patrons donated ${amount(amountSum)}. Monthly progress: ${buffer.last.percent}%"
        } >>- {
          buffer = Vector.empty
        }
      }
    }

    private def displayMessage(text: String) =
      zulip(_.general, "lila")(markdown.linkifyUsers(text))

    private def userAt(username: String) =
      if (username == "Anonymous") "Anonymous"
      else s"@$username"

    private def amount(cents: Int) = s"$$${BigDecimal(cents.toLong, 2)}"
  }
}

object IrcApi {

  sealed trait ModDomain
  object ModDomain {
    case object Admin extends ModDomain
    case object Cheat extends ModDomain
    case object Boost extends ModDomain
    case object Comm  extends ModDomain
    case object Other extends ModDomain
  }

  private val userRegex = lila.common.String.atUsernameRegex.pattern
  private val postRegex = lila.common.String.forumPostPathRegex.pattern

  private object markdown {
    def link(url: String, name: String)         = s"[$name]($url)"
    def lichessLink(path: String, name: String) = s"[$name](https://lichess.org$path)"
    def userLink(name: String): String          = lichessLink(s"/@/$name?mod&notes", name)
    def userLink(user: User): String            = userLink(user.username)
    def modLink(name: String): String           = lichessLink(s"/@/$name", name)
    def modLink(user: User): String             = modLink(user.username)
    def gameLink(id: String)                    = lichessLink(s"/$id", s"#$id")
    def userNotesLink(name: String)             = lichessLink(s"/@/$name?notes", "notes")
    def broadcastLink(id: String, name: String) = lichessLink(s"/broadcast/-/$id", name)
    def linkifyUsers(msg: String)               = userRegex matcher msg replaceAll (m => userLink(m.group(1)))
    val postReplace                             = lichessLink("/forum/$1", "$1")
    def linkifyPosts(msg: String)               = postRegex matcher msg replaceAll postReplace
    def linkifyPostsAndUsers(msg: String)       = linkifyPosts(linkifyUsers(msg))
    def fixImageUrl(url: String)                = url.replace("/display?", "/display.jpg?")
  }
}
