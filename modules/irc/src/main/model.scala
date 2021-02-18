package lila.irc

private case class SlackMessage(
    username: String,
    text: String,
    icon: String,
    channel: String
) {

  override def toString = s"[$channel] :$icon: @$username: $text"
}

private case class DiscordMessage(
    text: String,
    channel: Double
) {

  override def toString = s"[$channel] $text"
}
