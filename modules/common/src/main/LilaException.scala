package lila.common

import ornicar.scalalib

trait LilaException extends Exception {
  val message: String

  override def getMessage = message
  override def toString = message
}

object LilaException extends scalalib.Validation {

  def apply(msg: String): LilaException = new LilaException {
    val message = msg
  }

  def apply(msg: Failures): LilaException = apply(msg.shows)
}

trait ValidException extends LilaException {
  val message: String

  override def getMessage: String = "LilaValid['" + message + "']"
}

object ValidException extends scalalib.Validation {

  def apply(msg: String): ValidException = new ValidException {
    val message = msg
  }

  def apply(msg: Failures): ValidException = apply(msg.shows)
}
