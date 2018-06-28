package lila.base

import java.util.Base64
import java.lang.{ StringBuilder => jStringBuilder }
import scala.util.Try

final class PimpedTryList[A](private val list: List[Try[A]]) extends AnyVal {
  def sequence: Try[List[A]] = Try(list map { _.get })
}

final class PimpedList[A](private val list: List[A]) extends AnyVal {
  def sortLike[B](other: List[B], f: A => B): List[A] = list.sortWith {
    (x, y) => other.indexOf(f(x)) < other.indexOf(f(y))
  }
}

final class PimpedChars(private val iter: Iterable[CharSequence]) extends AnyVal {
  def concat: String = {
    val it = iter.iterator
    if (it.hasNext) {
      val first = it.next
      if (it.hasNext) {
        val sb = new jStringBuilder(first)
        do {
          sb.append(it.next)
        } while (it.hasNext)
        sb
      } else first
    }.toString
    else ""
  }
}

final class PimpedSeq[A](private val seq: Seq[A]) extends AnyVal {
  def has(a: A) = seq contains a
}

final class PimpedByteArray(private val self: Array[Byte]) extends AnyVal {
  def toBase64 = Base64.getEncoder.encodeToString(self)
}