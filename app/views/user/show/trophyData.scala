package views.user
package show

import play.api.libs.json.{ JsArray, Json, JsObject }

import lila.app.UiEnv.{ *, given }
import lila.app.mashup.UserInfo
import lila.user.Plan.sinceDate
import lila.user.TrophyKind

object trophyData:

  private def item(
      cssClass: String,
      title: String,
      href: Option[String] = None,
      icon: Option[String] = None,
      imgSrc: Option[String] = None,
      imgW: Option[Int] = None,
      imgH: Option[Int] = None,
      stacked: Boolean = false,
      badge: Boolean = false,
      primary: Boolean = true
  ): JsObject =
    Json.obj(
      "cls" -> cssClass,
      "title" -> title
    ) ++ JsObject(
      List(
        href.map("href" -> Json.toJson(_)),
        icon.map("icon" -> Json.toJson(_)),
        imgSrc.map("imgSrc" -> Json.toJson(_)),
        imgW.map("imgW" -> Json.toJson(_)),
        imgH.map("imgH" -> Json.toJson(_)),
        stacked.option("stacked" -> Json.toJson(true)),
        badge.option("badge" -> Json.toJson(true)),
        (!primary).option("primary" -> Json.toJson(false))
      ).flatten
    )

  def jsonList(u: User, info: UserInfo)(using ctx: Context): JsArray =
    val items = List.newBuilder[JsObject]

    if !u.lame then
      info.ranks.toList
        .sortBy(_._2)
        .foreach: (perf, rank) =>
          val ptype = lila.rating.PerfType(perf)
          bits
            .trophyMeta(ptype, rank)
            .foreach: (cssClass, title, imgPath) =>
              items += item(
                cssClass = cssClass,
                title = title,
                href = routes.User.top(ptype.key).url.some,
                imgSrc = assetUrl(imgPath).value.some
              )

    val fireTrophies = info.trophies.trophies
      .filter(_.kind.klass.has("fire-trophy"))
      .distinctBy(_.kind._id)
      .sorted

    fireTrophies.zipWithIndex.foreach: (trophy, idx) =>
      trophy.kind.icon.foreach: iconChar =>
        items += item(
          cssClass = trophyClass(trophy),
          title = trophy.kind.name,
          href = trophy.anyUrl,
          icon = iconChar.some,
          stacked = true,
          primary = idx == 0
        )

    info.trophies.shields.foreach: shield =>
      items += item(
        cssClass = "shield-trophy combo-trophy",
        title = s"${shield.categ.name} Shield",
        href = routes.Tournament.shields.url.some,
        icon = shield.categ.icon.value.some
      )

    info.trophies.revolutions.foreach: revol =>
      items += item(
        cssClass = "revol_trophy combo-trophy",
        title = s"${revol.variant.name} Revolution",
        href = routes.Tournament.show(revol.tourId).url.some,
        icon = revol.iconChar.value.some
      )

    info.trophies.trophies
      .find(_.kind._id == TrophyKind.zugMiracle)
      .foreach: t =>
        items += item(
          cssClass = trophyClass(t),
          title = t.kind.name,
          href = t.anyUrl,
          imgSrc = assetUrl("images/trophy/zug-trophy.png").value.some
        )

    info.trophies.trophies
      .filter(_.kind.withCustomImage)
      .foreach: t =>
        items += item(
          cssClass = trophyClass(t),
          title = t.kind.name,
          href = t.anyUrl,
          imgSrc = assetUrl(s"images/trophy/${t.kind._id}.png").value.some,
          imgW = 65.some,
          imgH = 80.some
        )

    info.trophies.trophies
      .filter(_.kind.klass.has("icon3d"))
      .distinctBy(_.kind._id)
      .sorted
      .foreach: trophy =>
        trophy.kind.icon.foreach: iconChar =>
          items += item(
            cssClass = trophyClass(trophy),
            title = trophy.kind.name,
            href = trophy.anyUrl,
            icon = iconChar.some,
            badge = true
          )

    if info.isCoach then
      items += item(
        cssClass = "trophy award icon3d coach",
        title = trans.coach.lichessCoach.txt(),
        href = routes.Coach.show(info.user.username).url.some,
        icon = lila.ui.Icon.GraduateCap.value.some,
        badge = true
      )

    if info.isStreamer && ctx.kid.no then
      val streaming = isStreaming(info.user.id)
      items += item(
        cssClass = s"trophy award icon3d streamer${if streaming then " streaming" else ""}",
        title = if streaming then "Live now!" else "Lichess Streamer",
        href = routes.Streamer.show(info.user.username, redirect = streaming).url.some,
        icon = lila.ui.Icon.Mic.value.some,
        badge = true
      )

    if u.plan.active then
      items += item(
        cssClass = "trophy award patron icon3d",
        title = trans.patron.patronSince.txt(showDate(u.plan.sinceDate)),
        href = routes.Plan.index().url.some,
        icon = patronIconChar.value.some,
        badge = true
      )

    JsArray(items.result())

  private def trophyClass(t: lila.user.Trophy): String =
    s"trophy award ${t.kind._id} ${~t.kind.klass}"
