package lila.ui

import java.time.LocalDate

import ScalatagsTemplate.{ *, given }
import lila.core.config.RouteUrl

final class AtomUi(routeUrl: RouteUrl):

  def feed[A](
      elems: Seq[A],
      htmlCall: Call,
      atomCall: Call,
      title: String,
      updated: Option[Instant]
  )(elem: A => Frag) =
    frag(
      raw("""<?xml version="1.0" encoding="utf-8"?>"""),
      raw(
        """<feed xml:lang="en-US" xmlns="http://www.w3.org/2005/Atom" xmlns:media="http://search.yahoo.com/mrss/">"""
      ),
      tag("id")(routeUrl(htmlCall)),
      link(rel := "alternate", tpe := "text/html", href := routeUrl(htmlCall)),
      link(rel := "self", tpe := "application/atom+xml", href := routeUrl(atomCall)),
      tag("title")(title),
      tag("updated")(updated.map(atomDate)),
      elems.map: el =>
        tag("entry")(elem(el)),
      raw("</feed>")
    )

  def atomDate(date: Instant): String = isoDateTimeFormatter.print(date)
  def atomDate(date: LocalDate): String =
    java.time.format.DateTimeFormatter.ISO_DATE.withZone(utcZone).print(date)

  def atomLink(url: Call) = a(
    cls := "atom",
    st.title := "Atom RSS feed",
    href := url,
    dataIcon := Icon.RssFeed
  )

  private val termAttr = attr("term")
  private val labelAttr = attr("label")
  private val schemeAttr = attr("scheme")

  def category(term: String, label: String, scheme: Option[Url] = None) =
    tag("category")(
      termAttr := term,
      labelAttr := label,
      schemeAttr := scheme
    )
