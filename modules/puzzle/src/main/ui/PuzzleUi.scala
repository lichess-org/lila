package lila.puzzle
package ui

import play.api.libs.json.Json
import chess.format.{ BoardFen, Uci }

import lila.ui.*
import ScalatagsTemplate.{ *, given }
import lila.common.LilaOpeningFamily
import lila.core.i18n.I18nKey
import scalalib.paginator.Paginator

final class PuzzleUi(helpers: Helpers, bits: PuzzleBits):
  import helpers.{ *, given }

  object theme:

    def list(all: PuzzleAngle.All)(using ctx: Context) =
      main(cls := "page-menu")(
        bits.pageMenu("themes", ctx.me),
        div(cls := "page-menu__content box")(
          h1(cls := "box__top")(trans.puzzle.puzzleThemes()),
          standardFlash.map(div(cls := "box__pad")(_)),
          div(cls := "puzzle-themes")(
            all.themes.take(2).map(themeCategory),
            h2(id := "openings")(
              "By game opening",
              a(href := routes.Puzzle.openings())(trans.site.more(), " »")
            ),
            opening.listOf(all.openings.families.take(12)),
            all.themes.drop(2).map(themeCategory),
            info
          )
        )
      )

    def info(using Context) =
      p(cls := "puzzle-themes__db text", dataIcon := Icon.Heart):
        trans.puzzleTheme.puzzleDownloadInformation:
          a(href := "https://database.lichess.org/")("database.lichess.org")

    private def themeCategory(cat: I18nKey, themes: List[PuzzleTheme.WithCount])(using Context) =
      frag(
        h2(id := cat.value)(cat()),
        div(cls := s"puzzle-themes__list ${cat.value.replace(":", "-")}")(
          themes.map: pt =>
            val url =
              if pt.theme == PuzzleTheme.mix then routes.Puzzle.home
              else routes.Puzzle.show(pt.theme.key.value)
            a(
              cls  := "puzzle-themes__link",
              href := (pt.count > 0).option(langHref(url))
            )(
              img(src := assetUrl(s"images/puzzle-themes/${iconFile(pt.theme.key)}.svg")),
              span(
                h3(
                  pt.theme.name(),
                  em(cls := "puzzle-themes__count")(pt.count.localize)
                ),
                span(pt.theme.description())
              )
            )
          ,
          (cat.value == "puzzle:origin").option(
            a(cls := "puzzle-themes__link", href := routes.Puzzle.ofPlayer())(
              img(src := assetUrl("images/puzzle-themes/playerGames.svg")),
              span(
                h3(trans.puzzleTheme.playerGames()),
                span(trans.puzzleTheme.playerGamesDescription())
              )
            )
          )
        )
      )

    private def iconFile(theme: PuzzleTheme.Key): String =
      import PuzzleTheme.*
      if theme.value.startsWith("mateIn") then "mate"
      else theme.value

  object opening:

    def all(openings: PuzzleOpeningCollection, mine: Option[PuzzleOpening.Mine], order: PuzzleOpening.Order)(
        using ctx: Context
    ) =
      main(cls := "page-menu")(
        bits.pageMenu("openings", ctx.me),
        div(cls := "page-menu__content box")(
          boxTop(
            h1(trans.puzzle.puzzlesByOpenings()),
            orderSelect(order)
          ),
          mine.isEmpty.option(
            frag(
              p(cls := "help help-touchscreen")(
                iconTag(Icon.InfoCircle, trans.puzzle.useFindInPage())
              ),
              p(cls := "help help-keyboard")(iconTag(Icon.InfoCircle, trans.puzzle.useCtrlF()))
            )
          ),
          div(cls := "puzzle-themes")(
            div(cls := "puzzle-openings")(
              mine.filter(_.families.nonEmpty).map { m =>
                div(cls := "puzzle-openings__mine")(
                  h2(trans.puzzle.openingsYouPlayedTheMost()),
                  div(cls := "puzzle-openings__list")(m.families.take(12).map {
                    familyLink(_, mine)(cls := "puzzle-openings__link")
                  })
                )
              },
              treeOf(openings.treeList(order), mine),
              theme.info
            )
          )
        )
      )

    def listOf(families: List[PuzzleOpening.FamilyWithCount])(using Context) =
      div(cls := "puzzle-openings__list"):
        families.map: fam =>
          a(cls := "puzzle-openings__link", href := routes.Puzzle.show(fam.family.key.value)):
            h3(fam.family.name, em(fam.count.localize))

    private def treeOf(openings: PuzzleOpening.TreeList, mine: Option[PuzzleOpening.Mine])(using Context) =
      openings.map: (fam, openings) =>
        div(cls := "puzzle-openings__tree__family")(
          h2(
            familyLink(fam.family, mine),
            em(fam.count.localize)
          ),
          openings.nonEmpty.option(div(cls := "puzzle-openings__list"):
            openings.map: op =>
              a(
                dataFen := op.opening.ref.fen,
                cls := List(
                  "blpt puzzle-openings__link" -> true,
                  "opening-mine"               -> mine.exists(_.variationKeys(op.opening.key))
                ),
                href := routes.Puzzle.show(op.opening.key.value)
              ):
                h3(op.opening.variation, em(op.count.localize))
          )
        )

    private def familyLink(family: LilaOpeningFamily, mine: Option[PuzzleOpening.Mine]): Tag = a(
      cls     := List("blpt" -> true, "opening-mine" -> mine.exists(_.familyKeys(family.key))),
      dataFen := family.full.map(_.fen)
    )(href := routes.Puzzle.show(family.key.value))(family.name)

    def orderSelect(order: PuzzleOpening.Order)(using Context) =
      lila.ui.bits.mselect(
        "orders",
        span(order.name()),
        PuzzleOpening.Order.list.map: o =>
          a(href := routes.Puzzle.openings(o.key), cls := (order == o).option("current"))(o.name())
      )

  def ofPlayer(query: String, user: Option[User], puzzles: Option[Paginator[Puzzle]])(using ctx: Context) =
    main(cls := "page-menu")(
      bits.pageMenu("player", user),
      div(cls := "page-menu__content puzzle-of-player box box-pad")(
        form(
          action := routes.Puzzle.ofPlayer(),
          method := "get",
          cls    := "form3 puzzle-of-player__form complete-parent"
        )(
          st.input(
            name         := "name",
            value        := query,
            cls          := "form-control user-autocomplete",
            placeholder  := trans.clas.lichessUsername.txt(),
            autocomplete := "off",
            dataTag      := "span",
            autofocus
          ),
          submitButton(cls := "button")(trans.puzzle.searchPuzzles.txt())
        ),
        div(cls := "puzzle-of-player__results")(
          (user, puzzles) match
            case (Some(u), Some(pager)) =>
              if pager.nbResults == 0 && ctx.is(u) then p(trans.puzzle.fromMyGamesNone())
              else
                frag(
                  p(strong(trans.puzzle.fromXGamesFound((pager.nbResults), userLink(u)))),
                  div(cls := "puzzle-of-player__pager infinite-scroll")(
                    pager.currentPageResults.map { puzzle =>
                      div(cls := "puzzle-of-player__puzzle")(
                        chessgroundMini(
                          fen = puzzle.fenAfterInitialMove.board,
                          color = puzzle.color,
                          lastMove = puzzle.line.head.some
                        )(
                          a(
                            cls  := s"puzzle-of-player__puzzle__board",
                            href := routes.Puzzle.show(puzzle.id)
                          )
                        ),
                        span(cls := "puzzle-of-player__puzzle__meta")(
                          span(cls := "puzzle-of-player__puzzle__id", s"#${puzzle.id}"),
                          span(cls := "puzzle-of-player__puzzle__rating", puzzle.glicko.intRating)
                        )
                      )
                    },
                    pagerNext(pager, np => s"${routes.Puzzle.ofPlayer(u.username.some, np).url}")
                  )
                )
            case (_, _) => emptyFrag
        )
      )
    )

  object history:
    import lila.puzzle.PuzzleHistory.{ PuzzleSession, SessionRound }

    def apply(user: User, pager: Paginator[PuzzleSession])(using ctx: Context) =
      val title =
        if ctx.is(user) then trans.puzzle.history.txt()
        else s"${user.username} ${trans.puzzle.history.txt()}"
      main(cls := "page-menu")(
        bits.pageMenu("history", user.some),
        div(cls := "page-menu__content box box-pad")(
          h1(cls := "box__top")(title),
          div(cls := "puzzle-history")(
            div(cls := "infinite-scroll")(
              pager.currentPageResults.map(renderSession),
              pagerNext(pager, np => routes.Puzzle.history(np, user.username.some).url)
            )
          )
        )
      )

    private def renderSession(session: PuzzleSession)(using Context) =
      div(cls := "puzzle-history__session")(
        h2(cls := "puzzle-history__session__title")(
          strong(PuzzleTheme(session.theme).name()),
          momentFromNow(session.puzzles.head.round.date)
        ),
        div(cls := "puzzle-history__session__rounds")(session.puzzles.toList.reverse.map(renderRound))
      )

    private def renderRound(r: SessionRound)(using Context) =
      a(cls := "puzzle-history__round", href := routes.Puzzle.show(r.puzzle.id))(
        chessgroundMini(r.puzzle.fenAfterInitialMove.board, r.puzzle.color, r.puzzle.line.head.some)(
          span(cls := "puzzle-history__round__puzzle")
        ),
        span(cls := "puzzle-history__round__meta")(
          span(cls := "puzzle-history__round__result")(
            if r.round.win.yes then goodTag(trans.puzzle.solved())
            else badTag(trans.puzzle.failed())
          ),
          span(cls := "puzzle-history__round__id")(s"#${r.puzzle.id}")
        )
      )
