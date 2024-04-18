package lila.video

import lila.core.net.Crawler

case class TagNb(_id: Tag, nb: Int):

  def tag = _id

  def empty = nb == 0

  def isNumeric = tag.forall(_.isDigit)

case class Filter(tags: List[String]):

  def toggle(tag: String) =
    copy(
      tags = if tags contains tag then tags.filter(tag !=) else tags :+ tag
    )

case class UserControl(
    filter: Filter,
    tags: List[TagNb],
    query: Option[String],
    crawler: Crawler
):

  def toggleTag(tag: String) =
    copy(
      filter = filter.toggle(tag),
      query = none
    )

  def queryString =
    List(
      filter.tags.nonEmpty.option(s"tags=${filter.tags.sorted.mkString("/")}".replace(' ', '+')),
      query.map { q =>
        s"q=$q"
      }
    ).flatten.mkString("&")

  def queryStringUnlessBot = crawler.no.so(queryString)
