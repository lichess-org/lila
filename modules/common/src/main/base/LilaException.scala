package lila.base

import ornicar.scalalib.ValidTypes._

trait LilaException extends Exception {
  val message: String

  override def getMessage = message
  override def toString   = message
}

trait ClientError extends LilaException

object LilaException extends scalaz.syntax.ToShowOps {

  def apply(msg: String) = new LilaException {
    val message = msg
  }

  def apply(msg: Failures): LilaException = apply(msg.shows)

  def client(msg: String) = new ClientError {
    val message = msg
  }
}
