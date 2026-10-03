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
  val byName = values.mapBy(_.name)

enum Quality:
  case spam, weak, good
  def name = toString

object Quality:
  val byName = values.mapBy(_.name) + ("great" -> good)

enum QualityFilter:
  case all, best, weak, spam, pending
  def name = toString

object QualityFilter:
  val byName = values.mapBy(_.name)
