package views.user
package show

import play.api.libs.json.{ JsArray, Json, JsObject }

import lila.app.UiEnv.{ *, given }
import lila.app.mashup.UserInfo
import lila.user.Plan.sinceDate
import lila.user.{ Trophy, TrophyKind }

object trophyData:

  private def item(
      cssClass: String,
      title: String,
      href: Option[String],
      icon: Option[String] = None,
      imgSrc: Option[String] = None,
      imgW: Option[Int] = None,
      imgH: Option[Int] = None,
      stacked: Boolean = false,
      badge: Boolean = false,
      primary: Boolean = true
  ): JsObject =
    Json
      .obj("cls" -> cssClass, "title" -> title)
      .add("href", href)
      .add("icon", icon)
      .add("imgSrc", imgSrc)
      .add("imgW", imgW)
      .add("imgH", imgH)
      .add("stacked", stacked.option(true))
      .add("badge", badge.option(true))
      .add("primary", (!primary).option(false))

  def jsonList(u: User, info: UserInfo)(using ctx: Context): JsArray =
    val items = List.newBuilder[JsObject]
    val t = info.trophies

    if !u.lame then
      t.ranks.toList
        .sortBy(_._2)
        .foreach: (perf, rank) =>
          val ptype = lila.rating.PerfType(perf)
          bits
            .trophyMeta(ptype, rank)
            .foreach: (c, title, img, w, h) =>
              items += item(
                c,
                title,
                href = routes.User.top(ptype.key).url.some,
                imgSrc = assetUrl(img).value.some,
                imgW = w.some,
                imgH = h.some
              )

    val trophies = t.trophies

    trophies
      .filter(_.kind.klass.has("fire-trophy"))
      .distinctBy(_.kind._id)
      .sorted
      .zipWithIndex
      .foreach: (tr, idx) =>
        tr.kind.icon.foreach: c =>
          items += item(
            trophyClass(tr),
            tr.kind.name,
            href = tr.anyUrl,
            icon = c.some,
            stacked = true,
            primary = idx == 0
          )

    t.shields.foreach: s =>
      items += item(
        "shield-trophy combo-trophy",
        s"${s.categ.name} Shield",
        href = routes.Tournament.shields.url.some,
        icon = s.categ.icon.value.some
      )

    t.revolutions.foreach: r =>
      items += item(
        "revol_trophy combo-trophy",
        s"${r.variant.name} Revolution",
        href = routes.Tournament.show(r.tourId).url.some,
        icon = r.iconChar.value.some
      )

    trophies
      .findLast(_.kind._id == TrophyKind.zugMiracle)
      .foreach: tr =>
        items += item(
          trophyClass(tr),
          tr.kind.name,
          href = tr.anyUrl,
          imgSrc = assetUrl("images/trophy/zug-trophy.png").value.some,
          imgW = 34.some,
          imgH = 60.some
        )

    trophies
      .filter(_.kind.withCustomImage)
      .distinctBy(_.kind._id)
      .foreach: tr =>
        items += item(
          trophyClass(tr),
          tr.kind.name,
          href = tr.anyUrl,
          imgSrc = assetUrl(s"images/trophy/${tr.kind._id}.png").value.some,
          imgW = 65.some,
          imgH = 80.some
        )

    trophies
      .filter(_.kind.klass.has("icon3d"))
      .distinctBy(_.kind._id)
      .sorted
      .foreach: tr =>
        tr.kind.icon.foreach: c =>
          items += item(trophyClass(tr), tr.kind.name, href = tr.anyUrl, icon = c.some, badge = true)

    if info.isCoach then
      items += item(
        "trophy award icon3d coach",
        trans.coach.lichessCoach.txt(),
        href = routes.Coach.show(info.user.username).url.some,
        icon = lila.ui.Icon.GraduateCap.value.some,
        badge = true
      )

    if info.isStreamer && ctx.kid.no then
      val streaming = isStreaming(info.user.id)
      items += item(
        s"trophy award icon3d streamer${if streaming then " streaming" else ""}",
        if streaming then "Live now!" else "Lichess Streamer",
        href = routes.Streamer.show(info.user.username, redirect = streaming).url.some,
        icon = lila.ui.Icon.Mic.value.some,
        badge = true
      )

    if u.plan.active then
      items += item(
        "trophy award patron icon3d",
        trans.patron.patronSince.txt(showDate(u.plan.sinceDate)),
        href = routes.Plan.index().url.some,
        icon = patronIconChar.value.some,
        badge = true
      )

    JsArray(items.result())

  private def trophyClass(t: lila.user.Trophy): String =
    s"trophy award ${t.kind._id} ${~t.kind.klass}"
