package lila.video

import org.joda.time.DateTime

case class TagNb(_id: Tag, nb: Int) {

  def tag = _id

  def noSpace = tag.replace(" ", "-")
}

case class Filter(
    _id: String, // user ID
    tags: List[String],
    date: DateTime) {

  def id = _id

  def toggle(tag: String) = copy(
    tags = if (tags contains tag) tags filter (tag!=) else tags :+ tag
  )

  def tagString = tags mkString ","
}

case class UserControl(
  filter: Filter,
  tags: List[TagNb])
