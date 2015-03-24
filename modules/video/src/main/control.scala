package lila.video

import org.joda.time.DateTime

case class TagNb(_id: Tag, nb: Int) {

  def tag = _id

  def empty = nb == 0
}

case class Filter(tags: List[String]) {

  def toggle(tag: String) = copy(
    tags = if (tags contains tag) tags filter (tag!=) else tags :+ tag
  )

  def queryString = s"tags=${tags mkString ","}".replace(" ", "+")
}

case class UserControl(
  filter: Filter,
  tags: List[TagNb])
