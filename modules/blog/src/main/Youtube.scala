package lila.blog

object Youtube:

  def augmentEmbeds(html: Html): Html =
    addCredentialless(fixStartTimes(html))

  private val IframeRegex = """(<iframe[^>]*)>""".r
  private def addCredentialless(html: Html) = Html:
    IframeRegex.replaceAllIn(html.value, """$1 credentialless>""")

  private val TimeMarkerRegex = """youtube\.com/watch\?v=[\w-]++\#t=([^"]++)[^?]++\?feature=oembed""".r
  private val HourMinSecRegex = """(\d++)h(\d++)m(\d++)s""".r
  private val MinSecRegex     = """(\d++)m(\d++)s""".r
  private val SecRegex        = """(\d++)s""".r
  /* <div data-oembed="https://www.youtube.com/watch?v=uz-dZ2W4Bf0#t=4m14s" data-oembed-type="video" data-oembed-provider="youtube"><iframe width="480" height="270" src="https://www.youtube.com/embed/uz-dZ2W4Bf0?feature=oembed" frameborder="0" allowfullscreen></iframe></div>
   * <div data-oembed="https://www.youtube.com/watch?v=uz-dZ2W4Bf0#t=4m14s" data-oembed-type="video" data-oembed-provider="youtube"><iframe width="480" height="270" src="https://www.youtube.com/embed/uz-dZ2W4Bf0?feature=oembed&start=254" frameborder="0" allowfullscreen></iframe></div>
   */
  private def fixStartTimes(html: Html) = Html:
    TimeMarkerRegex.replaceAllIn(
      html.value,
      m =>
        val orig = m group 0
        parseSeconds(m group 1).fold(orig)(seconds => s"$orig&start=$seconds")
    )

  private def parseSeconds(text: String) = text match
    case HourMinSecRegex(hourS, minS, secS) =>
      for
        hour <- hourS.toIntOption
        min  <- minS.toIntOption
        sec  <- secS.toIntOption
      yield 3600 * hour + 60 * min + sec
    case MinSecRegex(minS, secS) =>
      for
        min <- minS.toIntOption
        sec <- secS.toIntOption
      yield 60 * min + sec
    case SecRegex(secS) => secS.toIntOption
    case _              => None
