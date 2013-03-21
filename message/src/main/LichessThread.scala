package lila.message

case class LichessThread(
    to: String,
    subject: String,
    message: String) {

  def toThread: Thread = Threads.make(
    name = subject,
    text = message,
    creatorId = "lichess",
    invitedId = to)
}
