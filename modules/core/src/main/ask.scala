package lila.core
package ask

import lila.core.userId.UserId

trait AskApi:
  def encode(text: String, creator: UserId): Encoded
  def encodeAndCommit(text: String, creator: UserId, url: Option[String] = None): Fu[String]
  def decode(text: String): Fu[String]

trait Encoded:
  def text: String
  def commit(url: Option[String] = None): Funit

object AskMarkup:
  val markerIdPrefix = "\ufdd6\ufdd4\ufdd2\ufdd0"
  val markerIdRe = s"$markerIdPrefix\\{(\\S{8})}".r

  def strip(text: String, n: Int = -1): String =
    markerIdRe.replaceAllIn(text, "").take(if n == -1 then text.length else n)