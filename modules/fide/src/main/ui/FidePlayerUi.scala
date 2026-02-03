package lila.fide
package ui

import scalalib.paginator.Paginator

import lila.core.fide.FidePlayerOrder
import lila.ui.*
import lila.ui.ScalatagsTemplate.{ *, given }
import lila.core.id.ImageId

final class FidePlayerUi(helpers: Helpers, fideUi: FideUi, picfitUrl: lila.memo.PicfitUrl):
  import helpers.{ *, given }
  import trans.{ site as trs, broadcast as trb }

  def index(players: Paginator[FidePlayer.WithFollow], query: String, order: FidePlayerOrder)(using
      Context
  ) =
    fideUi.page("FIDE players", "players")(
      cls := "fide-players",
      boxTop(
        h1(trb.fidePlayers()),
        div(cls := "box__top__actions"):
          searchForm(query)
      ),
      playerList(
        players,
        order,
        np => routes.Fide.index(np, query.some.filter(_.nonEmpty)),
        sortable = query.isEmpty
      )
    )

  def notFound(id: chess.FideId)(using Context) =
    fideUi.page("FIDE player not found", "players")(
      cls := "fide-players",
      boxTop(
        h1(trb.fidePlayerNotFound()),
        div(cls := "box__top__actions"):
          searchForm("")
      ),
      div(cls := "box__pad")(
        p(
          "We could not find anyone with the FIDE ID \"",
          strong(id),
          "\", please make sure the number is correct."
        ),
        p(
          "If the player appears on the ",
          a(href := "https://ratings.fide.com/", targetBlank)("official FIDE website"),
          ", then the player was not included in the latest rating export from FIDE.",
          br,
          "FIDE exports are provided once a month and includes players who have at least one official rating."
        )
      )
    )

  def searchForm(q: String) =
    st.form(cls := "fide-players__search-form", action := routes.Fide.index(), method := "get")(
      input(
        cls := "fide-players__search-form__input",
        name := "q",
        st.placeholder := "Search for players",
        st.value := q,
        autofocus := true,
        autocomplete := "off",
        spellcheck := "false"
      ),
      submitButton(cls := "button", dataIcon := Icon.Search)
    )

  def playerList(
      players: Paginator[FidePlayer.WithFollow],
      order: FidePlayerOrder,
      url: Int => Call,
      sortable: Boolean
  )(using ctx: Context) =
    def header(label: Frag, o: FidePlayerOrder) =
      if sortable then
        val current = o == order
        th(
          a(
            href := current.not.option(addQueryParam(url(1).url, "order", o.key)),
            cls := List("active" -> current)
          )(label)
        )
      else th(label)
    table(
      cls := List("slist slist-pad fide-players-table" -> true, "fide-players-table--sortable" -> sortable)
    )(
      thead:
        tr(
          header(trs.name(), FidePlayerOrder.name),
          header(trs.classical(), FidePlayerOrder.standard),
          header(trs.rapid(), FidePlayerOrder.rapid),
          header(trs.blitz(), FidePlayerOrder.blitz),
          header(trb.age(), FidePlayerOrder.year),
          ctx.isAuth.option(header(trs.follow(), FidePlayerOrder.follow))
        )
      ,
      tbody(cls := "infinite-scroll")(
        players.currentPageResults.map: p =>
          val player = p.player
          val link = a(href := routes.Fide.show(player.id, player.slug))
          tr(cls := "paginated")(
            td(cls := "player-intro-td")(
              span(cls := "player-intro")(
                link(cls := "player-intro__photo"):
                  player.photo
                    .fold(thumbnail.fallback(cls := "fide-players__photo fide-players__photo--fallback")):
                      photo => img(src := thumbnail.url(photo.id, _.Small), cls := "fide-players__photo")
                ,
                span(cls := "player-intro__info")(
                  link(cls := "player-intro__name")(titleTag(player.title), player.name),
                  player.fed.map: fed =>
                    span(cls := "player-intro__fed")(
                      fideUi.federation.flag(fed, none),
                      Federation.names.get(fed)
                    )
                )
              )
            ),
            td(player.standard),
            td(player.rapid),
            td(player.blitz),
            td(player.age),
            ctx.isAuth.option(td(followButton(p)))
          )
        ,
        pagerNextTable(players, np => addQueryParam(url(np).url, "order", order.key))
      )
    )

  private def followButton(p: FidePlayer.WithFollow) =
    val id = s"fide-player-follow-${p.player.id}"
    label(cls := "fide-player__follow")(
      form3.cmnToggle(
        fieldId = id,
        fieldName = id,
        checked = p.follow,
        action = Some(routes.Fide.follow(p.player.id, p.follow).url),
        cssClass = "cmn-favourite"
      )
    )

  def show(
      player: FidePlayer,
      user: Option[User],
      tours: Option[Frag],
      ratings: FideRatingHistory,
      isFollowing: Boolean
  )(using ctx: Context) =
    fideUi.page(
      s"${player.name} - FIDE player ${player.id}",
      "players",
      _.js(esmInit("fideRatingChart", ratings.toJson))
    )(
      cls := "box-pad fide-player",
      div(cls := "fide-player__header")(
        player.photo.map: photo =>
          div(cls := "fide-player__photo")(
            img(src := thumbnail.url(photo.id, _.Medium)),
            photo.credit.map: credit =>
              span(cls := "fide-player__photo__credit")("Credit: ", credit)
          ),
        div(cls := "fide-player__header__info")(
          h1(cls := "fide-player__header__name")(
            span(titleTag(player.title), player.name),
            user.map(userLink(_, withTitle = false)(cls := "fide-player__user"))
          ),
          ctx.isAuth.option(followButton(FidePlayer.WithFollow(player, isFollowing))(trans.site.follow())),
          table(cls := "fide-player__header__table")(
            tbody(
              player.fed.map: fed =>
                tr(
                  th(trb.federation()),
                  td(
                    a(
                      cls := "fide-player__federation",
                      href := routes.Fide.federation(Federation.idToSlug(fed))
                    )(
                      fideUi.federation.flag(fed, none),
                      Federation.name(fed)
                    )
                  )
                ),
              tr(
                th(trb.fideProfile()),
                td(a(href := s"https://ratings.fide.com/profile/${player.id}")(player.id))
              ),
              tr(
                th(trb.age()),
                td(
                  player.age,
                  for by <- player.year; dy <- player.deceasedYear
                  yield s" ($by - $dy)"
                )
              )
            )
          )
        )
      ),
      Granter.opt(_.FidePlayer).option(photoForm(player)),
      div(cls := "fide-player__ratings")(
        fideUi.tcTrans.map: (tc, name, icon) =>
          div(cls := "fide-player__rating")(
            div(cls := "fide-player__rating__text")(
              em(dataIcon := icon, cls := "text")(name()),
              strong(player.ratingOf(tc).fold(trb.unrated())(_.toString))
            ),
            canvas(cls := s"fide-player__rating__history fide-player__rating__history--$tc")
          )
      ),
      tours.map: tours =>
        div(cls := "fide-player__tours")(h2(trb.recentTournaments()), tours)
    )

  object thumbnail:
    def apply(image: Option[ImageId], size: FidePlayer.PlayerPhoto.SizeSelector): Tag =
      image.fold(fallback): id =>
        img(src := url(id, size))
    def fallback = img(src := staticAssetUrl("images/anon-face.webp"))
    def url(id: ImageId, size: FidePlayer.PlayerPhoto.SizeSelector) =
      FidePlayer.PlayerPhoto(picfitUrl, id, size)

  private def photoForm(player: FidePlayer)(using ctx: Context) =
    val credit = lila.fide.FidePlayer.form.credit(player)
    form3.fieldset("Photo", toggle = player.photo.isEmpty.some)(
      postForm(cls := "form3 fide-player__form", action := routes.Fide.playerUpdate(player.id))(
        form3.split(
          div(
            cls := "form-group form-half fide-player__photo-edit",
            data("post-url") := routes.Fide.playerPhoto(player.id)
          )(
            p(
              "Portrait of the player. It should be recognizable even at small sizes. ",
              strong(trans.streamer.maxSize(s"${lila.memo.PicfitApi.uploadMaxMb}MB."))
            ),
            form3.file.selectImage()
          ),
          form3.group(credit("photo.credit"), trans.ublog.imageCredit(), half = true)(form3.input(_))
        ),
        form3.action(form3.submit("Save"))
      )
    )
