package lila.base

import java.util.Base64
import scala.util.Try
import cats.data.NonEmptyList

final class LilaTryList[A](private val list: List[Try[A]]) extends AnyVal {
  def sequence: Try[List[A]] = Try(list map { _.get })
}

final class LilaList[A](private val list: List[A]) extends AnyVal {
  def sortLike[B](other: List[B], f: A => B): List[A] =
    list.sortWith { (x, y) =>
      other.indexOf(f(x)) < other.indexOf(f(y))
    }
  def toNel: Option[NonEmptyList[A]] =
    list match {
      case Nil           => None
      case first :: rest => Some(NonEmptyList(first, rest))
    }
  def tailOption: Option[List[A]] = list match {
    case Nil       => None
    case _ :: rest => Some(rest)
  }
  def tailSafe: List[A]         = tailOption getOrElse Nil
  def indexOption(a: A)         = Option(list indexOf a).filter(0 <=)
  def previous(a: A): Option[A] = indexOption(a).flatMap(i => list.lift(i - 1))
  def next(a: A): Option[A]     = indexOption(a).flatMap(i => list.lift(i + 1))
}

final class LilaSeq[A](private val seq: Seq[A]) extends AnyVal {
  def has(a: A) = seq contains a
}

final class LilaByteArray(private val self: Array[Byte]) extends AnyVal {
  def toBase64 = Base64.getEncoder.encodeToString(self)
}
