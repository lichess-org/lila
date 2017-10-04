package lila.common.base

import ornicar.scalalib
import scala.concurrent.Future
import lila.common.LilaException

trait LilaTypes extends scalalib.Validation {
  type Fu[+A] = Future[A]
  type Funit = Fu[Unit]

  def fuccess[A](a: A) = Future successful a
  def fufail[A <: Throwable, B](a: A): Fu[B] = Future failed a
  def fufail[A](a: String): Fu[A] = fufail(LilaException(a))
  def fufail[A](a: Failures): Fu[A] = fufail(LilaException(a))
  val funit = fuccess(())
}

object LilaTypes extends LilaTypes