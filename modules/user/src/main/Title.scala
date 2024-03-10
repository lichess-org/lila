package lila.user

// #TODO use /modules/fide
object Title:

  ???

  // object fromUrl:
  //
  //   // https://ratings.fide.com/card.phtml?event=740411
  //   private val FideProfileUrlRegex = """(?:https?://)?ratings\.fide\.com/card\.phtml\?event=(\d+)""".r
  //   // >&nbsp;FIDE title</td><td colspan=3 bgcolor=#efefef>&nbsp;Grandmaster</td>
  //   private val FideProfileTitleRegex =
  //     s"""<div class="profile-top-info__block__row__data">(${names.values.mkString(
  //         "|"
  //       )})</div>""".r.unanchored
  //
  //   // https://ratings.fide.com/profile/740411
  //   private val NewFideProfileUrlRegex = """(?:https?://)?ratings\.fide\.com/profile/(\d+)""".r
  //
  //   import play.api.libs.ws.StandaloneWSClient
  //
  //   def toFideId(url: String): Option[Int] =
  //     url.trim match
  //       case FideProfileUrlRegex(id)    => id.toIntOption
  //       case NewFideProfileUrlRegex(id) => id.toIntOption
  //       case _                          => none
  //
  //   def apply(url: String)(using ws: StandaloneWSClient): Fu[Option[PlayerTitle]] =
  //     toFideId(url).so(fromFideProfile)
  //
  //   private def fromFideProfile(id: Int)(using ws: StandaloneWSClient): Fu[Option[PlayerTitle]] =
  //     ws.url(s"""https://ratings.fide.com/profile/$id""").get().dmap(_.body).dmap {
  //       case FideProfileTitleRegex(name) => fromNames.get(name)
  //       case _                           => none
  //     }
