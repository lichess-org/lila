package lila.pref
package ui
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class AccountUi(helpers: Helpers):
  import helpers.{ *, given }

  def AccountPage(title: String, active: String)(using lila.ui.Context) =
    Page(title)
      .css("user.account")
      .js(Esm("user.account"))
      .wrap: body =>
        main(cls := "account page-menu")(
          menu(active),
          div(cls := "page-menu__content")(body)
        )

  def categName(categ: PrefCateg)(using Translate): String = categ match
    case PrefCateg.Display => trans.preferences.display.txt()
    case PrefCateg.ChessClock => trans.preferences.chessClock.txt()
    case PrefCateg.GameBehavior => trans.preferences.gameBehavior.txt()
    case PrefCateg.Privacy => trans.preferences.privacy.txt()

  def setting(name: Frag, body: Frag) = st.section(h2(name), body)

  def radios[A](field: play.api.data.Field, options: Iterable[(A, String)]) =
    st.group(cls := "radio"):
      options.toList.map: (key, value) =>
        val id = s"ir${field.id}_$key"
        val checked = field.value.has(key.toString)
        div(
          input(
            st.id := id,
            checked.option(st.checked),
            tpe := "radio",
            st.value := key.toString,
            name := field.name
          ),
          label(`for` := id)(value)
        )

  def bitCheckboxes(field: play.api.data.Field, options: Iterable[(Int, String)]) =
    st.group(cls := "radio")(
      /// Will hold the value being calculated with the various checkboxes when sending
      div(
        input(
          st.id := s"ir${field.id}_hidden",
          true.option(st.checked),
          tpe := "hidden",
          st.value := "",
          name := field.name
        ),
        st.style := "display: none;"
      ) :: options
        .map: (key, value) =>
          val id = s"ir${field.id}_$key"
          val intVal = ~field.value.flatMap(_.toIntOption)
          val checked = (intVal & key) == key
          div(
            input(
              st.id := id,
              checked.option(st.checked),
              tpe := "checkbox",
              st.value := key.toString,
              attr("data-name") := field.name
            ),
            label(`for` := id)(value)
          )
        .toList
    )

  def menu(active: String)(using ctx: Context) =
    def activeCls(c: String) = cls := active.activeO(c)
    ctx.me
      .exists(_.enabled.yes)
      .option(
        lila.ui.bits.pageMenuSubnav(
          a(activeCls("editProfile"), href := routes.Account.profile)(
            trans.site.editProfile()
          ),
          div(cls := "sep"),
          PrefCateg.values.map: categ =>
            a(activeCls(categ.slug), href := routes.Pref.form(categ.slug))(
              categName(categ)
            ),
          a(activeCls("notification"), href := routes.Pref.form("notification"))(
            trans.site.notifications()
          ),
          a(activeCls("kid"), href := routes.Account.kid)(
            trans.site.kidMode()
          ),
          div(cls := "sep"),
          a(activeCls("username"), href := routes.Account.username)(
            trans.site.changeUsername()
          ),
          Granter
            .opt(_.Coach)
            .option(
              a(activeCls("coach"), href := routes.Coach.edit)(
                trans.coach.lichessCoach()
              )
            ),
          a(activeCls("password"), href := routes.Account.passwd)(
            trans.site.changePassword()
          ),
          a(activeCls("email"), href := routes.Account.email)(
            trans.site.changeEmail()
          ),
          a(activeCls("twofactor"), href := routes.Account.twoFactor)(
            trans.tfa.twoFactorAuth()
          ),
          a(activeCls("oauth.token"), href := routes.OAuthToken.index)(
            trans.oauthScope.apiAccessTokens()
          ),
          a(activeCls("security"), href := routes.Account.security)(
            trans.site.security()
          ),
          div(cls := "sep"),
          a(activeCls("network"), href := routes.Pref.network)(
            "Network"
          ),
          ctx.noBot.option(
            a(href := routes.DgtCtrl.index)(
              trans.dgt.dgtBoard()
            )
          ),
          div(cls := "sep"),
          a(activeCls("close"), href := routes.Account.close)(
            trans.settings.closeAccount()
          )
        )
      )
