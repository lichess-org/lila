package lila
package message

case class LichessThread(
    to: String,
    subject: String,
    message: String) {

  def toThread: Thread = Thread.make(
    name = subject,
    text = message,
    creatorId = "lichess",
    invitedId = to
  )
}
