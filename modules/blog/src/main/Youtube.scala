package lila.blog

object Youtube {

  private val EmbedRegex      = """youtube\.com/watch\?v=[\w-]++\#t=([^"]+).+\?feature=oembed""".r
  private val HourMinSecRegex = """(\d++)h(\d++)m(\d++)s""".r
  private val MinSecRegex     = """(\d++)m(\d++)s""".r
  private val SecRegex        = """(\d++)s""".r

  /*
   * <div data-oembed="https://www.youtube.com/watch?v=uz-dZ2W4Bf0#t=4m14s" data-oembed-type="video" data-oembed-provider="youtube"><iframe width="480" height="270" src="https://www.youtube.com/embed/uz-dZ2W4Bf0?feature=oembed" frameborder="0" allowfullscreen></iframe></div>
   * <div data-oembed="https://www.youtube.com/watch?v=uz-dZ2W4Bf0#t=4m14s" data-oembed-type="video" data-oembed-provider="youtube"><iframe width="480" height="270" src="https://www.youtube.com/embed/uz-dZ2W4Bf0?feature=oembed&start=254" frameborder="0" allowfullscreen></iframe></div>
   */
  def fixStartTimes(html: String) =
    EmbedRegex.replaceAllIn(
      html,
      m => {
        val orig = m group 0
        parseSeconds(m group 1).fold(orig)(seconds => s"$orig&start=$seconds")
      }
    )

  private def parseSeconds(text: String) =
    text match {
      case HourMinSecRegex(hourS, minS, secS) =>
        for {
          hour <- hourS.toIntOption
          min  <- minS.toIntOption
          sec  <- secS.toIntOption
        } yield 3600 * hour + 60 * min + sec
      case MinSecRegex(minS, secS) =>
        for {
          min <- minS.toIntOption
          sec <- secS.toIntOption
        } yield 60 * min + sec
      case SecRegex(secS) =>
        for {
          sec <- secS.toIntOption
        } yield sec
      case _ => None
    }
}
