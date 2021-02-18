package lila.irc

private case class SlackMessage(
    username: String,
    text: String,
    icon: String,
    channel: String
) {

  override def toString = s"[$channel] :$icon: @$username: $text"
}
