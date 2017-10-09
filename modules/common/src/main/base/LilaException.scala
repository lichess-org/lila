package lila.base

import ornicar.scalalib

trait LilaException extends Exception {
  val message: String

  override def getMessage = message
  override def toString = message
}

object LilaException extends scalalib.Validation
  with scalaz.syntax.ToShowOps {

  def apply(msg: String): LilaException = new LilaException {
    val message = msg
  }

  def apply(msg: Failures): LilaException = apply(msg.shows)
}