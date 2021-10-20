package lila.base

trait LilaException extends Exception {
  val message: String

  override def getMessage = message
  override def toString   = message
}

case class LilaInvalid(message: String) extends LilaException

object LilaException {

  def apply(msg: String) =
    new LilaException {
      val message = msg
    }
}
