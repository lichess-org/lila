package lila.core
package ublog

import lila.core.id.UblogPostId
import lila.core.userId.UserId

trait UblogPost:
  val id: UblogPostId
  val created: UblogPost.Recorded

object UblogPost:

  case class Recorded(by: UserId, at: Instant)

  case class Create(post: UblogPost) extends AnyVal

  case class LightPost(id: UblogPostId, title: String):
    def slug = scalalib.StringOps.slug(title)

trait UblogApi:
  def liveLightsByIds(ids: List[UblogPostId]): Fu[List[UblogPost.LightPost]]

enum BlogsBy:
  case newest, oldest, score, likes
  def name = toString

object BlogsBy:
  def fromName(name: String): Option[BlogsBy] = values.find(_.name == name)

enum Quality:
  case spam, weak, good, great
  def name = toString

object Quality:
  def fromName(name: String): Option[Quality] = values.find(_.name == name)

enum QualityFilter:
  case all, best, weak, spam
  def name = toString

object QualityFilter:
  def fromName(name: String): Option[QualityFilter] = values.find(_.name == name)
