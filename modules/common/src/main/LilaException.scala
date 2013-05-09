package lila.common

import ornicar.scalalib

trait LilaException extends Throwable {
  val message: String

  override def getMessage: String = "LilaError['" + message + "']"
}

object LilaException extends scalalib.Validation {

  def apply(msg: String): LilaException = new LilaException {
    val message = msg
  }

  def apply(msg: Failures): LilaException = apply(msg.shows)
}
