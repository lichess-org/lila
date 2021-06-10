package lila.irc

import lila.common.LightUser
import lila.user.User
import lila.user.Holder
import org.joda.time.DateTime

final class IrcApi(
    discord: DiscordClient,
    slack: SlackClient,
    zulip: ZulipClient,
    noteApi: lila.user.NoteApi,
    implicit val lightUser: LightUser.Getter
)(implicit ec: scala.concurrent.ExecutionContext) {

  import IrcApi._

  def commReportBurst(user: User): Funit = {
    val md = markdown.linkifyUsers(s"Burst of comm reports about @${user.username}")
    discord.comms(md) >> zulip.mod()(md)
  }

  def userMod(user: User, mod: Holder): Funit =
    noteApi
      .forMod(user.id)
      .map(_.headOption.filter(_.date isAfter DateTime.now.minusMinutes(5)))
      .flatMap {
        case None =>
          slack(
            SlackMessage(
              username = mod.user.username,
              icon = "eyes",
              text = s"Let's have a look at _*${slackdown.userLink(user.username)}*_",
              channel = SlackApi.rooms.tavern
            )
          ) >> zulip.mod()(
            s":eyes: ${markdown.userLink(mod.user.username)}: Let's have a look at **${markdown.userLink(user.username)}**"
          )
        case Some(note) =>
          slack(
            SlackMessage(
              username = mod.user.username,
              icon = "spiral_note_pad",
              text =
                s"_*${slackdown.userLink(user.username)}*_ (${slackdown.userNotesLink(user.username)}):\n" +
                  slackdown.linkifyUsers(note.text take 2000),
              channel = SlackApi.rooms.tavern
            )
          ) >> zulip.mod()(
            s":note: ${markdown.userLink(mod.user.username)}: **${markdown
              .userLink(user.username)}** (${markdown.userNotesLink(user.username)}):\n" +
              markdown.linkifyUsers(note.text take 2000)
          )
      }

  def userModNote(modName: String, username: String, note: String): Funit =
    slack(
      SlackMessage(
        username = modName,
        icon = "spiral_note_pad",
        text = s"_*${slackdown.userLink(username)}*_ (${slackdown.userNotesLink(username)}):\n" +
          slackdown.linkifyUsers(note take 2000),
        channel = SlackApi.rooms.tavernNotes
      )
    ) >>
      zulip.mod(ZulipClient.topic.notes)(
        s":note: ${markdown.userLink(modName)} **${markdown.userLink(username)}** (${markdown.userNotesLink(username)}):\n" +
          markdown.linkifyUsers(note take 2000)
      )
}

private object IrcApi {

  val userRegex = lila.common.String.atUsernameRegex.pattern

  object markdown { // both discord and zulip
    def link(url: String, name: String)         = s"[$name]($url)"
    def lichessLink(path: String, name: String) = s"[$name](https://lichess.org$path)"
    def userLink(name: String): String          = lichessLink(s"/@/$name?mod", name)
    def userLink(user: User): String            = userLink(user.username)
    def userNotesLink(name: String)             = lichessLink(s"/@/$name?notes", "notes")
    val userReplace                             = link("https://lichess.org/@/$1?mod", "$1")
    def linkifyUsers(msg: String) =
      userRegex matcher msg replaceAll userReplace
  }

  object slackdown { // special markup for slack
    def link(url: String, name: String)         = s"<$url|$name>"
    def lichessLink(path: String, name: String) = s"<https://lichess.org$path|$name>"
    def userLink(name: String): String          = lichessLink(s"/@/$name?mod", name)
    def userLink(user: User): String            = userLink(user.username)
    def gameLink(id: String)                    = lichessLink(s"/$id", s"#$id")
    def userNotesLink(name: String)             = lichessLink(s"/@/$name?notes", "notes")
    def broadcastLink(id: String, name: String) = lichessLink(s"/broadcast/-/$id", name)
    val chatPanicLink                           = lichessLink("mod/chat-panic", "Chat Panic")
    val userReplace                             = link("https://lichess.org/@/$1?mod", "$1")
    def linkifyUsers(msg: String) =
      userRegex matcher msg replaceAll userReplace
  }
}
