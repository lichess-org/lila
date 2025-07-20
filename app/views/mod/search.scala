package views.mod

import play.api.data.Form
import scalalib.paginator.Paginator

import lila.app.UiEnv.{ *, given }
import lila.core.net.IpAddress
import lila.core.security.FingerHash
import lila.mod.IpRender.RenderIp
import lila.security.IpTrust
import lila.user.WithPerfsAndEmails
import lila.mod.ModUserSearchResult
import lila.ui.bits.modMenu

object search:

  def apply(form: Form[?], res: Option[ModUserSearchResult])(using Context) =
    Page("Search users")
      .css("mod.misc")
      .js(Esm("mod.search")):
        main(cls := "page-menu")(
          modMenu("search"),
          div(cls := "mod-search page-menu__content box")(
            h1(cls := "box__top")("Search users"),
            st.form(cls := "search box__pad", action := routes.Mod.search, method := "GET")(
              input(
                name := "q",
                autofocus,
                placeholder := "Search by IP, email, or username (exact match only)",
                value       := form("q").value
              )
            ),
            res.map: r =>
              frag(
                ui.modUserSearchResult(r),
                userTable(r.users, showUsernames = true, eraseButton = isGranted(_.GdprErase))
              )
          )
        )

  def print(
      fh: FingerHash,
      users: List[WithPerfsAndEmails],
      uas: List[String],
      blocked: Boolean
  )(using Context) =
    Page("Fingerprint")
      .css("mod.misc")
      .js(Esm("mod.search")):
        main(cls := "page-menu")(
          modMenu("search"),
          div(cls := "mod-search page-menu__content box")(
            boxTop(
              h1("Fingerprint: ", fh.value),
              if isGranted(_.Admin) then
                postForm(cls := "box__top__actions", action := routes.Mod.printBan(!blocked, fh.value))(
                  submitButton(
                    cls := List(
                      "button text" -> true,
                      "active"      -> blocked
                    )
                  )(if blocked then "Banned" else "Ban this print")
                )
              else if blocked then div(cls := "banned")("BANNED")
              else emptyFrag
            ),
            userAgentsBox(uas),
            userTable(users)
          )
        )

  private def userAgentsBox(uas: List[String])(using Context) =
    isGranted(_.Admin).option(
      div(cls := "box__pad")(
        h2("User agents"),
        ul(cls := "mod-search__user-agents")(uas.map(li(_)))
      )
    )

  def ip(
      address: IpAddress,
      users: List[lila.user.WithPerfsAndEmails],
      data: IpTrust.IpData,
      blocked: Boolean
  )(using ctx: Context, renderIp: RenderIp) =
    Page("IP address")
      .css("mod.misc")
      .js(Esm("mod.search")):
        main(cls := "page-menu")(
          modMenu("search"),
          div(cls := "mod-search page-menu__content box")(
            boxTop(
              h1("IP address: ", renderIp(address)),
              if isGranted(_.Admin) then
                postForm(
                  cls    := "box__top__actions",
                  action := routes.Mod.singleIpBan(!blocked, address.value)
                )(
                  submitButton(
                    cls := List(
                      "button text" -> true,
                      "active"      -> blocked
                    )
                  )(if blocked then "Banned" else "Ban this IP")
                )
              else if blocked then div(cls := "banned")("BANNED")
              else emptyFrag
            ),
            div(cls := "mod-search__ip-data box__pad")(
              p(
                isGranted(_.Admin).option(frag("Location: ", data.location.toString, br)),
                "Proxy: ",
                data.proxy.toString
              )
            ),
            userTable(users)
          )
        )

  def clas(c: lila.clas.Clas, users: List[WithPerfsAndEmails])(using Context) =
    views.clas.ui.search.clas(c, userTable(users))

  export views.clas.ui.search.teacher

  def notes(query: String, pager: Paginator[lila.user.Note])(using Context) =
    views.user.noteUi.search(query, pager, modMenu("notes"))
