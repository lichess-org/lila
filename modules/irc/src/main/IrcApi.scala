package lila.irc

import lila.core.LightUser
import lila.core.LightUser.Me.given
import lila.core.irc.*
import lila.core.id.*

final class IrcApi(
    zulip: ZulipClient,
    noteApi: lila.core.user.NoteApi
)(using lightUser: LightUser.Getter, ec: Executor)
    extends lila.core.irc.IrcApi:

  import IrcApi.*

  def commReportBurst(user: LightUser): Funit =
    val md = markdown.linkifyUsers(s"Burst of comm reports about @${user.name}")
    zulip(_.mod.commsPrivate, "burst")(md)

  def inquiry(user: LightUser, domain: ModDomain, room: String)(using mod: LightUser.Me): Funit =
    val stream = domain match
      case ModDomain.Comm  => ZulipClient.stream.mod.commsPrivate
      case ModDomain.Cheat => ZulipClient.stream.mod.hunterCheat
      case ModDomain.Boost => ZulipClient.stream.mod.hunterBoost
      case _               => ZulipClient.stream.mod.adminGeneral
    noteApi
      .recentByUserForMod(user.id)
      .flatMap:
        case None =>
          zulip.sendAndGetLink(stream, "/" + user.name):
            val link = markdown.userLink(user.name)
            s"${markdown.userLink(mod.name)} :monkahmm: is looking at a $room report about **$link**"

        case Some(note) =>
          zulip.sendAndGetLink(stream, "/" + user.name):
            val link = markdown.userLink(user.name)
            s"${markdown.modLink(mod.name)} :pepenote: **$link** (${markdown.userNotesLink(user.name)}):\n" +
              markdown.linkifyUsers(note.text.take(2000))
      .flatMapz: zulipLink =>
        noteApi.write(
          user.id,
          s"$domain discussion: $zulipLink",
          modOnly = true,
          dox = domain == ModDomain.Admin
        )

  def nameCloseVote(user: LightUser, details: String, reason: Option[String])(using
      mod: LightUser.Me
  ): Funit =
    val topic = "/" + user.name
    zulip(_.mod.usernames, topic)(s"$details${reason.fold("")(r => s", reason: $r")}") >>
      zulip
        .sendAndGetLink(_.mod.usernames, topic)("/poll Close?\n🔨 Yes\n🍃 No")
        .flatMapz: zulipLink =>
          noteApi.write(
            user.id,
            s"name discussion: $zulipLink",
            modOnly = true,
            dox = false
          )

  def fullCommExport(user: LightUser)(using mod: LightUser.Me): Funit =
    val topic = "/" + user.name
    zulip(_.mod.trustSafety, topic):
      s"${markdown.modLink(mod.name)} exported all comms of ${markdown.userLink(user.name)}"

  def usertableCheck(user: LightUser)(using mod: LightUser.Me): Funit =
    zulip(_.mod.cafeteria, "reports"):
      s"**${markdown.userLinkNoNotes(user.name)}** usertable check (requested by ${markdown.modLink(mod.name)})"

  def commlog(user: LightUser, reportBy: Option[UserId])(using mod: LightUser.Me): Funit =
    zulip(_.mod.adminLog, "private comms checks"):
      val checkedOut =
        val finalS = if user.name.value.endsWith("s") then "" else "s"
        s"**${markdown.modLink(mod.name)}** checked out **${markdown.userLink(user.name)}**'$finalS communications "
      checkedOut + reportBy
        .filterNot(_.is(mod))
        .fold("spontaneously"): by =>
          s"while investigating a report created by ${markdown.userLink(by.into(UserName))}"

  def monitorMod(icon: String, text: String, tpe: ModDomain)(using modId: MyId): Funit =
    lightUser(modId).flatMapz: mod =>
      zulip(_.mod.adminMonitor(tpe), mod.name.value):
        s"${markdown.userLink(mod.name)} :$icon: ${markdown.linkifyPostsAndUsers(text)}"

  def chatPanic(v: Boolean)(using mod: LightUser.Me): Funit =
    val msg =
      s":stop: ${markdown.modLink(mod.name)} ${if v then "enabled" else "disabled"} ${markdown
          .lichessLink("/mod/chat-panic", " Chat Panic")}"
    zulip(_.mod.log, "chat panic")(msg) >> zulip(_.mod.commsPublic, "main")(msg)

  def broadcastError(id: RelayRoundId, name: String, error: String): Funit =
    zulip(_.broadcast, "lila error log")(s"${markdown.broadcastLink(id, name)} $error")

  def ublogPost(
      user: LightUser,
      id: UblogPostId,
      slug: String,
      title: String,
      intro: String
  ): Funit =
    zulip(_.blog, "non-tiered new posts"):
      val link = markdown.lichessLink(s"/@/${user.name}/blog/$slug/$id", title)
      s":note: $link $intro - by ${markdown.userLink(user)}"

  def broadcastStart(id: RelayRoundId, fullName: String): Funit =
    zulip(_.broadcast, "non-tiered broadcasts"):
      s":note: ${markdown.broadcastLink(id, fullName)}"

  def userAppeal(user: LightUser)(using mod: LightUser.Me): Funit =
    zulip
      .sendAndGetLink(_.mod.adminAppeal, "/" + user.name):
        val link = markdown.lichessLink(s"/appeal/${user.name}", user.name)
        s"${markdown.modLink(mod.name)} :monkahmm: is looking at the appeal of **$link**"
      .flatMapz: zulipAppealConv =>
        noteApi.write(user.id, s"Appeal discussion: $zulipAppealConv", modOnly = true, dox = true)

  def nameClosePreset(name: UserName): Funit =
    zulip(_.mod.adminGeneral, "name 48h closure"):
      s"@**remind** here in 48h to close ${markdown.userLink(name)}"

  def stop(): Funit = zulip(_.general, "lila")("Lichess is restarting.")

  def publishEvent(event: Event): Funit = event match
    case Event.Error(msg)   => publishError(msg)
    case Event.Warning(msg) => publishWarning(msg)
    case Event.Info(msg)    => publishInfo(msg)
    case Event.Victory(msg) => publishVictory(msg)

  private def publishError(msg: String): Funit =
    zulip(_.general, "lila")(s":lightning: ${markdown.linkifyUsers(msg)}")

  private def publishWarning(msg: String): Funit =
    zulip(_.general, "lila")(s":thinking: ${markdown.linkifyUsers(msg)}")

  private def publishVictory(msg: String): Funit =
    zulip(_.general, "lila")(s":tada: ${markdown.linkifyUsers(msg)}")

  private[irc] def publishInfo(msg: String): Funit =
    zulip(_.general, "lila")(s":info: ${markdown.linkifyUsers(msg)}")

  object charge:
    import lila.core.misc.plan.ChargeEvent
    private var buffer: Vector[ChargeEvent] = Vector.empty
    private given Ordering[ChargeEvent]     = Ordering.by[ChargeEvent, Int](_.cents)

    def apply(event: ChargeEvent): Funit =
      buffer = buffer :+ event
      buffer.head.date
        .isBefore(nowInstant.minusHours(12))
        .so:
          val firsts    = scalalib.HeapSort.topN(buffer, 10).map(_.username).map(userAt).mkString(", ")
          val amountSum = buffer.map(_.cents).sum
          val patrons =
            if firsts.lengthIs > 10
            then s"$firsts and, like, ${firsts.length - 10} others,"
            else firsts
          displayMessage {
            s"$patrons donated ${amount(amountSum)}. Monthly progress: ${buffer.last.percent}%"
          }.andDo {
            buffer = Vector.empty
          }

    private def displayMessage(text: String) =
      zulip(_.general, "lila")(markdown.linkifyUsers(text))

    private def userAt(name: UserName) =
      if name == UserName("Anonymous") then name
      else s"@$name"

    private def amount(cents: Int) = s"$$${BigDecimal(cents.toLong, 2)}"

object IrcApi:

  private val userRegex = lila.common.String.atUsernameRegex.pattern
  private val postRegex = lila.common.String.forumPostPathRegex.pattern

  private object markdown:
    def link(url: String, name: String)             = s"[$name]($url)"
    def lichessLink[N: Show](path: String, name: N) = show"[$name](https://lichess.org$path)"
    def userLink(name: UserName): String            = lichessLink(s"/@/$name?mod&notes", name.value)
    def userLink(user: LightUser): String           = userLink(user.name)
    def userLinkNoNotes(name: UserName): String     = lichessLink(s"/@/$name?mod", name.value)
    def userIdLinks(ids: List[UserId]): String =
      UserName.from[List, UserId](ids).map(markdown.userLink).mkString(", ")
    def modLink(name: UserName): String               = lichessLink(s"/@/$name", name.value)
    def gameLink(id: String)                          = lichessLink(s"/$id", s"#$id")
    def printLink(print: String)                      = lichessLink(s"/mod/print/$print", print)
    def ipLink(ip: String)                            = lichessLink(s"/mod/ip/$ip", ip)
    def userNotesLink(name: UserName)                 = lichessLink(s"/@/$name?notes", "notes")
    def broadcastLink(id: RelayRoundId, name: String) = lichessLink(s"/broadcast/-/-/$id", name)
    def linkifyUsers(msg: String) = userRegex.matcher(msg).replaceAll(m => userLink(UserName(m.group(1))))
    val postReplace               = lichessLink("/forum/$1", "$1")
    def linkifyPosts(msg: String) = postRegex.matcher(msg).replaceAll(postReplace)
    def linkifyPostsAndUsers(msg: String) = linkifyPosts(linkifyUsers(msg))
    def fixImageUrl(url: String)          = url.replace("/display?", "/display.jpg?")
