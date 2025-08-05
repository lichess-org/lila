package lila.user
package ui

import scalalib.paginator.Paginator

import lila.core.config.NetDomain
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class NoteUi(helpers: Helpers)(using NetDomain):
  import helpers.{ *, given }

  def zone(u: User, notes: List[Note])(using ctx: Context) = div(cls := "note-zone")(
    postForm(cls := "note-form", action := routes.User.writeNote(u.username))(
      form3.textarea(lila.user.UserForm.note("text"))(
        placeholder := trans.site.writeAPrivateNoteAboutThisUser.txt()
      ),
      if Granter.opt(_.ModNote) then
        div(cls := "mod-note")(
          submitButton(cls := "button", name := "noteType", value := "mod")("Save Mod Note"),
          Granter
            .opt(_.Admin)
            .option(
              submitButton(cls := "button", name := "noteType", value := "dox")(
                "Save Dox Note"
              )
            ),
          submitButton(cls := "button", name := "noteType", value := "normal")("Save Regular Note")
        )
      else submitButton(cls := "button", name := "noteType", value := "normal")(trans.site.save())
    ),
    notes.isEmpty.option(div(trans.site.noNoteYet())),
    notes.map: note =>
      div(cls := "note")(
        p(cls := "note__text")(richText(note.text, expandImg = false)),
        (note.mod && Granter.opt(_.Admin)).option(
          postForm(
            action := routes.User.setDoxNote(note._id, !note.dox)
          ):
            submitButton(cls := "button-empty yes-no-confirm button text")("Toggle Dox")
        ),
        p(cls := "note__meta")(
          userIdLink(note.from.some),
          br,
          note.dox.option("dox "),
          if Granter.opt(_.ModNote) then momentFromNowServer(note.date)
          else momentFromNow(note.date),
          (ctx.me.exists(note.isFrom) && !note.mod).option(
            frag(
              br,
              postForm(action := routes.User.deleteNote(note._id))(
                submitButton(
                  cls := "button-empty button-red yes-no-confirm button text",
                  style := "float:right",
                  dataIcon := Icon.Trash
                )(trans.site.delete())
              )
            )
          )
        )
      )
  )

  def search(query: String, pager: Paginator[Note], menu: Frag)(using Context) =
    Page("Mod notes")
      .css("mod.misc")
      .css("bits.slist")
      .js(infiniteScrollEsmInit):
        main(cls := "page-menu")(
          menu,
          div(cls := "page-menu__content box")(
            boxTop(
              h1("Mod notes"),
              div(cls := "box__top__actions")(
                st.form(cls := "search", action := routes.Mod.notes())(
                  input(st.name := "q", value := query, placeholder := trans.search.search.txt())
                )
              )
            ),
            br,
            br,
            table(cls := "slist slist-pad")(
              thead(
                tr(th("Moderator"), th("Player"), th("Note"), th("Date"))
              ),
              tbody(cls := "infinite-scroll")(
                pager.currentPageResults.map: note =>
                  tr(cls := "paginated")(
                    td(userIdLink(note.from.some)),
                    td(userIdLink(note.to.some, params = "?notes=1")),
                    td(cls := "user-note__text")(richText(note.text, expandImg = false)),
                    td(small(momentFromNowOnce(note.date)))
                  ),
                pagerNextTable(pager, np => routes.Mod.notes(np, query).url)
              )
            )
          )
        )
