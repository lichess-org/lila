package views.html.base

import play.api.mvc.Call

import lila.app.templating.Environment.*
import lila.app.ui.ScalatagsTemplate.{ *, given }
import java.time.LocalDate

object atom:

  def apply[A](
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
      tag("id")(s"$netBaseUrl$htmlCall"),
      link(rel := "alternate", tpe := "text/html", href            := s"${netBaseUrl}$htmlCall"),
      link(rel := "self", tpe      := "application/atom+xml", href := s"${netBaseUrl}$atomCall"),
      tag("title")(title),
      tag("updated")(updated map atomDate),
      elems.map: el =>
        tag("entry")(elem(el)),
      raw("</feed>")
    )

  def atomDate(date: Instant): String = isoDateTimeFormatter print date
  def atomDate(date: LocalDate): String =
    java.time.format.DateTimeFormatter.ISO_DATE.withZone(utcZone) print date

  private val termAttr   = attr("term")
  private val labelAttr  = attr("label")
  private val schemeAttr = attr("scheme")

  def category(term: String, label: String, scheme: Option[String] = None) =
    tag("category")(
      termAttr   := term,
      labelAttr  := label,
      schemeAttr := scheme
    )
