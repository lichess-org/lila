package lidraughts.slack

private case class SlackMessage(
    username: String,
    text: String,
    icon: String,
    channel: String
)
