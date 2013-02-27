package lila
package message

case class LichessThread(
    to: String,
    subject: String,
    message: String) {

  def toThread: Thread = Thread(
    name = subject,
    text = message,
    creator = "lichess",
    invited = to
  )
}
