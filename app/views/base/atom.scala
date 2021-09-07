package views.html.base

import controllers.routes
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import play.api.mvc.Call

import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.config.BaseUrl

object atom {

  def apply[A](
      elems: Seq[A],
      htmlCall: Call,
      atomCall: Call,
      title: String,
      updated: Option[DateTime]
  )(elem: A => Frag) =
    frag(
      raw("""<?xml version="1.0" encoding="UTF-8"?>"""),
      raw(
        """<feed xml:lang="en-US" xmlns="http://www.w3.org/2005/Atom" xmlns:media="http://search.yahoo.com/mrss/">"""
      ),
      tag("id")(s"$netBaseUrl$htmlCall"),
      link(rel := "alternate", tpe := "text/html", href := s"${netBaseUrl}$htmlCall"),
      link(rel := "self", tpe := "application/atom+xml", href := s"${netBaseUrl}$atomCall"),
      tag("title")(title),
      tag("updated")(updated map atomDate),
      elems.map { el =>
        tag("entry")(elem(el))
      },
      raw("</feed>")
    )

  private val atomDateFormatter        = ISODateTimeFormat.dateTime
  def atomDate(date: DateTime): String = atomDateFormatter print date
}
