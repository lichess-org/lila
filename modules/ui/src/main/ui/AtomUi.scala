package lila.ui

import java.time.LocalDate
import scalalib.model.Language

import lila.ui.ScalatagsTemplate.{ *, given }
import lila.core.config.RouteUrl
import lila.core.i18n.defaultLang

final class AtomUi(routeUrl: RouteUrl):

  def feed[A](
      elems: Seq[A],
      htmlCall: Call,
      atomCall: Call,
      title: String,
      updated: Option[Instant],
      language: A => Language
  )(elem: A => Frag) =
    val feedLang = elems.headOption.fold(defaultLang)(language)
    frag(
      raw("""<?xml version="1.0" encoding="utf-8"?>"""),
      raw(
        s"""<feed xml:lang="$feedLang" xmlns="http://www.w3.org/2005/Atom" xmlns:media="http://search.yahoo.com/mrss/">"""
      ),
      tag("id")(routeUrl(htmlCall)),
      link(rel := "alternate", tpe := "text/html", href := routeUrl(htmlCall)),
      link(rel := "self", tpe := "application/atom+xml", href := routeUrl(atomCall)),
      tag("title")(title),
      tag("updated")(updated.map(atomDate)),
      elems.map: el =>
        entryTag(language(el))(elem(el)),
      raw("</feed>")
    )

  def entryTag(lang: Language) = tag("entry")(attr("xml:lang") := lang)

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
