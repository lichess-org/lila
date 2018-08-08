package lidraughts.base

import ornicar.scalalib.ValidTypes._

trait LidraughtsException extends Exception {
  val message: String

  override def getMessage = message
  override def toString = message
}

object LidraughtsException extends scalaz.syntax.ToShowOps {

  def apply(msg: String) = new LidraughtsException {
    val message = msg
  }

  def apply(msg: Failures): LidraughtsException = apply(msg.shows)
}