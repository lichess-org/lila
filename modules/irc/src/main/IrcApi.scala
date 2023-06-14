package lila.irc

import lila.common.{ EmailAddress, Heapsort, IpAddress, LightUser }
import lila.hub.actorApi.irc.*
import lila.user.Holder
import lila.user.User
import cats.Show
import cats.syntax.show.*

final class IrcApi(
    zulip: ZulipClient,
    noteApi: lila.user.NoteApi
)(using lightUser: LightUser.Getter, ec: Executor):

  import IrcApi.*

  def commReportBurst(user: User): Funit =
    val md = markdown.linkifyUsers(s"Burst of comm reports about @${user.username}")
    zulip(_.mod.commsPrivate, "burst")(md)

  def inquiry(user: User, mod: Holder, domain: ModDomain, room: String): Funit =
    val stream = domain match
      case ModDomain.Comm  => ZulipClient.stream.mod.commsPrivate
      case ModDomain.Cheat => ZulipClient.stream.mod.hunterCheat
      case ModDomain.Boost => ZulipClient.stream.mod.hunterBoost
      case _               => ZulipClient.stream.mod.adminGeneral
    noteApi
      .byUserForMod(user.id)
      .map(_.headOption.filter(_.date isAfter nowInstant.minusMinutes(5)))
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
      .flatMapz { zulipLink =>
        noteApi.write(
          user,
          s"$domain discussion: $zulipLink",
          mod.user,
          modOnly = true,
          dox = domain == ModDomain.Admin
        )
      }

  def nameCloseVote(user: User, mod: Holder): Funit =
    zulip
      .sendAndGetLink(_.mod.usernames, "/" + user.username)("/poll Close?\n🔨 Yes\n🍃 No")
      .flatMapz { zulipLink =>
        noteApi.write(
          user,
          s"username discussion: $zulipLink",
          mod.user,
          modOnly = true,
          dox = false
        )
      }

  def usertableCheck(user: User, mod: Holder): Funit =
    zulip(_.mod.cafeteria, "reports")(
      s"**${markdown.userLinkNoNotes(user.username)}** usertable check (requested by ${markdown.modLink(mod.user.username)})"
    )

  def userModNote(modName: UserName, username: UserName, note: String): Funit =
    (!User.isLichess(modName)).so:
      zulip(_.mod.adminLog, "notes"):
        s"${markdown.modLink(modName)} :note: **${markdown.userLink(username)}** (${markdown.userNotesLink(username)}):\n" +
          markdown.linkifyUsers(note take 2000)

  def selfReport(typ: String, path: String, user: User, ip: IpAddress): Funit =
    zulip(_.mod.adminLog, "self report")(
      s"[**$typ**] ${markdown.userLink(user)}@$ip ${markdown.gameLink(path)}"
    )

  def commlog(mod: Holder, user: User, reportBy: Option[UserId]): Funit =
    zulip(_.mod.adminLog, "private comms checks")({
      val finalS = if (user.username.value endsWith "s") "" else "s"
      s"**${markdown modLink mod.user}** checked out **${markdown userLink user.username}**'$finalS communications "
    } + reportBy.filter(mod.id !=).fold("spontaneously") { by =>
      s"while investigating a report created by ${markdown.userLink(by into UserName)}"
    })

  def monitorMod(modId: UserId, icon: String, text: String, tpe: ModDomain): Funit =
    lightUser(modId) flatMapz { mod =>
      zulip(_.mod.adminMonitor(tpe), mod.name.value)(
        s"${markdown.userLink(mod.name)} :$icon: ${markdown.linkifyPostsAndUsers(text)}"
      )
    }

  def logMod(modId: UserId, icon: String, text: String): Funit =
    lightUser(modId) flatMapz { mod =>
      zulip(_.mod.log, "actions")(
        s"${markdown.modLink(mod.name)} :$icon: ${markdown.linkifyPostsAndUsers(text)}"
      )
    }

  def printBan(mod: Holder, print: String, v: Boolean, userIds: List[UserId]): Funit =
    logMod(
      mod.id,
      "paw_prints",
      s"${if (v) "Banned" else "Unbanned"} print ${markdown
          .printLink(print)} of ${userIds.length} user(s): ${markdown userIdLinks userIds}"
    )

  def ipBan(mod: Holder, ip: String, v: Boolean, userIds: List[UserId]): Funit =
    logMod(
      mod.id,
      "1234",
      s"${if (v) "Banned" else "Unbanned"} IP ${markdown
          .ipLink(ip)} of ${userIds.length} user(s): ${markdown userIdLinks userIds}"
    )

  def chatPanic(mod: Holder, v: Boolean): Funit =
    val msg =
      s":stop: ${markdown.modLink(mod.user)} ${if (v) "enabled" else "disabled"} ${markdown.lichessLink("/mod/chat-panic", " Chat Panic")}"
    zulip(_.mod.log, "chat panic")(msg) >> zulip(_.mod.commsPublic, "main")(msg)

  def garbageCollector(msg: String): Funit =
    zulip(_.mod.adminLog, "garbage collector")(markdown linkifyUsers msg)

  def broadcastError(id: RelayRoundId, name: String, error: String): Funit =
    zulip(_.broadcast, "lila error log")(s"${markdown.broadcastLink(id, name)} $error")

  def ublogPost(
      user: User,
      id: UblogPostId,
      slug: String,
      title: String,
      intro: String
  ): Funit =
    zulip(_.blog, "non-tiered new posts"):
      s":note: ${markdown
          .lichessLink(s"/@/${user.username}/blog/$slug/$id", title)} $intro - by ${markdown
          .userLink(user)}"

  def broadcastStart(id: RelayRoundId, fullName: String): Funit =
    zulip(_.broadcast, "non-tiered broadcasts"):
      s":note: ${markdown.broadcastLink(id, fullName)}"

  def userAppeal(user: User, mod: Holder): Funit =
    zulip
      .sendAndGetLink(_.mod.adminAppeal, "/" + user.username):
        s"${markdown.modLink(mod.user)} :monkahmm: is looking at the appeal of **${markdown
            .lichessLink(s"/appeal/${user.username}", user.username)}**"
      .flatMapz: zulipAppealConv =>
        noteApi.write(user, s"Appeal discussion: $zulipAppealConv", mod.user, modOnly = true, dox = true)

  def nameClosePreset(username: UserName): Funit =
    zulip(_.mod.usernames, s"/$username")("@**remind** here in 48h to close this account")

  def stop(): Funit = zulip(_.general, "lila")("Lichess is restarting.")

  def publishEvent(event: Event): Funit = event match
    case Event.Error(msg)   => publishError(msg)
    case Event.Warning(msg) => publishWarning(msg)
    case Event.Info(msg)    => publishInfo(msg)
    case Event.Victory(msg) => publishVictory(msg)

  def signupAfterTryingDisposableEmail(user: User, email: EmailAddress, previous: Set[EmailAddress]) =
    zulip(_.mod.adminLog, "disposable email")(
      s"${markdown userLink user} signed up with ${email.value} after trying: ${previous mkString ", "}"
    )

  private def publishError(msg: String): Funit =
    zulip(_.general, "lila")(s":lightning: ${markdown linkifyUsers msg}")

  private def publishWarning(msg: String): Funit =
    zulip(_.general, "lila")(s":thinking: ${markdown linkifyUsers msg}")

  private def publishVictory(msg: String): Funit =
    zulip(_.general, "lila")(s":tada: ${markdown linkifyUsers msg}")

  private[irc] def publishInfo(msg: String): Funit =
    zulip(_.general, "lila")(s":info: ${markdown linkifyUsers msg}")

  object charge:
    import lila.hub.actorApi.plan.ChargeEvent
    private var buffer: Vector[ChargeEvent] = Vector.empty
    private given Ordering[ChargeEvent]     = Ordering.by[ChargeEvent, Int](_.cents)

    def apply(event: ChargeEvent): Funit =
      buffer = buffer :+ event
      buffer.head.date.isBefore(nowInstant.minusHours(12)) so {
        val firsts    = Heapsort.topN(buffer, 10).map(_.username).map(userAt).mkString(", ")
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

    private def displayMessage(text: String) =
      zulip(_.general, "lila")(markdown.linkifyUsers(text))

    private def userAt(username: UserName) =
      if (username == UserName("Anonymous")) username
      else s"@$username"

    private def amount(cents: Int) = s"$$${BigDecimal(cents.toLong, 2)}"

object IrcApi:

  enum ModDomain:
    case Admin, Cheat, Boost, Comm, Other

  private val userRegex = lila.common.String.atUsernameRegex.pattern
  private val postRegex = lila.common.String.forumPostPathRegex.pattern

  private object markdown:
    def link(url: String, name: String)             = s"[$name]($url)"
    def lichessLink[N: Show](path: String, name: N) = show"[$name](https://lichess.org$path)"
    def userLink(name: UserName): String            = lichessLink(s"/@/$name?mod&notes", name.value)
    def userLink(user: User): String                = userLink(user.username)
    def userLinkNoNotes(name: UserName): String     = lichessLink(s"/@/$name?mod", name.value)
    def userIdLinks(ids: List[UserId]): String =
      UserName.from[List, UserId](ids) map markdown.userLink mkString ", "
    def modLink(name: UserName): String               = lichessLink(s"/@/$name", name.value)
    def modLink(user: User): String                   = modLink(user.username)
    def gameLink(id: String)                          = lichessLink(s"/$id", s"#$id")
    def printLink(print: String)                      = lichessLink(s"/mod/print/$print", print)
    def ipLink(ip: String)                            = lichessLink(s"/mod/ip/$ip", ip)
    def userNotesLink(name: UserName)                 = lichessLink(s"/@/$name?notes", "notes")
    def broadcastLink(id: RelayRoundId, name: String) = lichessLink(s"/broadcast/-/-/$id", name)
    def linkifyUsers(msg: String) = userRegex matcher msg replaceAll (m => userLink(UserName(m.group(1))))
    val postReplace               = lichessLink("/forum/$1", "$1")
    def linkifyPosts(msg: String) = postRegex matcher msg replaceAll postReplace
    def linkifyPostsAndUsers(msg: String) = linkifyPosts(linkifyUsers(msg))
    def fixImageUrl(url: String)          = url.replace("/display?", "/display.jpg?")
