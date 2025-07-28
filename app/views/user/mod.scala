package views.user

import lila.app.UiEnv.{ *, given }
import lila.appeal.Appeal
import lila.mod.IpRender.RenderIp
import lila.mod.UserWithModlog
import lila.mod.ui.{ mzSection, ModUserTableUi }
import lila.security.{ Dated, UserAgentParser, UserClient, UserLogins }

object mod:

  import views.mod.user.*

  def student(managed: lila.clas.Student.ManagedInfo)(using Context): Frag =
    mzSection("student")(
      "Created by ",
      userLink(managed.createdBy),
      " for class ",
      a(href := routes.Clas.show(managed.clas.id))(managed.clas.name)
    )

  def boardTokens(tokens: List[lila.oauth.AccessToken]): Frag =
    if tokens.isEmpty then emptyFrag
    else
      mzSection("boardTokens")(
        strong(cls := "inline")(pluralize("Board token", tokens.size)),
        ul:
          tokens.map: token =>
            li(
              List(token.description, token.clientOrigin).flatten.mkString(" "),
              ", last used ",
              token.usedAt.map(momentFromNowOnce)
            )
      )

  def plan(u: User)(charges: List[lila.plan.Charge])(using Context): Option[Frag] =
    charges.nonEmpty.option(
      mzSection("plan")(
        strong(cls := "text inline", dataIcon := patronIconChar)(
          "Patron payments",
          Granter.opt(_.PayPal).option {
            charges.find(_.giftTo.isEmpty).flatMap(_.payPal).flatMap(_.subId).map { subId =>
              frag(
                " - ",
                a(
                  href := s"https://www.paypal.com/fr/cgi-bin/webscr?cmd=_profile-recurring-payments&encrypted_profile_id=$subId"
                )("[PayPal sub]")
              )
            }
          }
        ),
        ul(
          charges.map { c =>
            li(
              c.giftTo match
                case Some(giftedId) if u.is(giftedId) => frag("Gift from", userIdLink(c.userId), " ")
                case Some(giftedId) => frag("Gift to", userIdLink(giftedId.some), " ")
                case _ => emptyFrag
              ,
              c.money.display,
              " with ",
              c.serviceName,
              " on ",
              showInstant(c.date),
              " UTC"
            )
          }
        ),
        br
      )
    )

  def otherUsers(u: User, data: UserLogins.TableData[UserWithModlog], appeals: List[Appeal])(using
      ctx: Context,
      renderIp: lila.mod.IpRender.RenderIp
  ): Tag =
    import data.*
    val canLocate = Granter.opt(_.Admin)
    mzSection("others")(
      table(cls := "slist")(
        thead(
          tr(
            th(
              pluralize("linked user", userLogins.otherUsers.size),
              (max < 1000 || othersPartiallyLoaded).option(
                frag(
                  nbsp,
                  a(cls := "more-others")("Load more")
                )
              )
            ),
            Granter.opt(_.Admin).option(th("Email")),
            thSortNumber(dataSortDefault)("Same"),
            th("Games"),
            thSortNumber(playban)(cls := "i", title := "Playban"),
            thSortNumber(alt)(cls := "i", title := "Alt"),
            thSortNumber(shadowban)(cls := "i", title := "Shadowban"),
            thSortNumber(boosting)(cls := "i", title := "Boosting"),
            thSortNumber(engine)(cls := "i", title := "Engine"),
            thSortNumber(closed)(cls := "i", title := "Closed"),
            thSortNumber(reportban)(cls := "i", title := "Reportban"),
            thSortNumber(notesText)(cls := "i", title := "Notes"),
            thSortNumber(iconTag(Icon.InkQuill))(cls := "i", title := "Appeals"),
            thSortNumber("Created"),
            thSortNumber("Active"),
            ModUserTableUi.selectAltAll
          )
        ),
        tbody(
          othersWithEmail.others.map { case other @ UserLogins.OtherUser(log @ UserWithModlog(o, _), _, _) =>
            val userNotes = notes.filter: n =>
              n.to.is(o.id) && (ctx.me.exists(n.isFrom) || Granter.opt(_.Admin))
            val userAppeal = appeals.find(_.isAbout(o.id))
            tr(
              dataTags := List(
                other.ips.map(renderIp),
                other.fps,
                canLocate.so(userLogins.distinctLocationIdsOf(other.ips))
              ).flatten.mkString(" "),
              cls := o.is(u).option("same")
            )(
              if o.is(u) || lila.security.Granter.canViewAltUsername(o)
              then td(dataSort := o.id)(userLink(o, withPerfRating = o.perfs.some, params = "?mod"))
              else td,
              Granter.opt(_.Admin).option(td(emailValueOf(othersWithEmail)(o))),
              td(
                cls := "ips-prints",
                // show prints and ips separately
                dataSort := other.score + (other.ips.nonEmpty.so(1000000)) + (other.fps.nonEmpty.so(3000000))
              )(
                List(other.ips.size -> "IP", other.fps.size -> "Print")
                  .collect:
                    case (nb, name) if nb > 0 => s"$nb $name"
                  .mkString(", ")
              ),
              td(dataSort := o.count.game)(o.count.game.localize),
              markTd(~bans.get(o.id), playban(cls := "text")(~bans.get(o.id): Int)),
              markTd(o.marks.alt.so(1), alt, log.dateOf(_.alt)),
              markTd(o.marks.troll.so(1), shadowban, log.dateOf(_.troll)),
              markTd(o.marks.boost.so(1), boosting, log.dateOf(_.booster)),
              markTd(o.marks.engine.so(1), engine, log.dateOf(_.engine)),
              markTd(o.enabled.no.so(1), closed, log.dateOf(_.closeAccount)),
              markTd(o.marks.reportban.so(1), reportban, log.dateOf(_.reportban)),
              userNotes.nonEmpty
                .option {
                  td(dataSort := userNotes.size)(
                    a(href := s"${routes.User.show(o.username)}?notes")(
                      notesText(
                        title := s"Notes from ${userNotes.map(_.from).map(titleNameOrId).mkString(", ")}",
                        cls := "is-green"
                      ),
                      userNotes.size
                    )
                  )
                }
                .getOrElse(td(dataSort := 0)),
              userAppeal match
                case None => td(dataSort := 0)
                case Some(appeal) =>
                  td(dataSort := 1)(
                    a(
                      href := Granter.opt(_.Appeals).option(routes.Appeal.show(o.username).url),
                      cls := List(
                        "text" -> true,
                        "appeal-muted" -> appeal.isMuted
                      ),
                      dataIcon := Icon.InkQuill,
                      title := s"${pluralize("appeal message", appeal.msgs.size)}${appeal.isMuted.so(" [MUTED]")}"
                    )(appeal.msgs.size)
                  )
              ,
              td(dataSort := o.createdAt.toMillis)(momentFromNowServer(o.createdAt)),
              td(dataSort := o.seenAt.map(_.toMillis.toString))(o.seenAt.map(momentFromNowServer)),
              ModUserTableUi.userCheckboxTd(o.marks.alt)
            )
          }
        )
      )
    )

  private def emailValueOf(emails: UserLogins.WithMeSortedWithEmails[UserWithModlog])(u: User) =
    emails.emails
      .get(u.id)
      .map(_.value)
      .map:
        case EmailAddress.clasIdRegex(id) =>
          a(href := routes.Clas.show(lila.core.id.ClasId(id)))(s"Class #$id")
        case email => frag(email)

  def identification(logins: UserLogins, othersPartiallyLoaded: Boolean)(using
      ctx: Context,
      renderIp: RenderIp
  ): Frag =
    val canIpBan = Granter.opt(_.IpBan)
    val canFpBan = Granter.opt(_.PrintBan)
    val canLocate = Granter.opt(_.Admin)
    mzSection("identification")(
      canLocate.option(
        div(cls := "spy_locs")(
          table(cls := "slist slist--sort spy_filter")(
            thead(
              tr(
                th("Country"),
                th("Proxy"),
                th("Region"),
                th("City"),
                thSortNumber("Date")
              )
            ),
            tbody(
              logins.distinctLocations.toList
                .sortBy(-_.seconds)
                .map: l =>
                  tr(dataValue := l.value.location.id)(
                    td(l.value.location.country),
                    td(l.value.proxy.name.map { proxy => span(cls := "proxy", title := "Proxy")(proxy) }),
                    td(l.value.location.region),
                    td(l.value.location.city),
                    td(dataSort := l.date.toMillis)(momentFromNowServer(l.date))
                  )
                .toList
            )
          )
        )
      ),
      canLocate.option(
        div(cls := "spy_uas")(
          table(cls := "slist slist--sort")(
            thead(
              tr(
                th(pluralize("Device", logins.uas.size)),
                th("OS"),
                th("Client"),
                thSortNumber("Date"),
                th("Type")
              )
            ),
            tbody(
              logins.uas
                .sortBy(-_.seconds)
                .map { case Dated(ua, date) =>
                  val parsed = UserAgentParser.parse(ua)
                  tr(
                    td(title := ua.value)(
                      if parsed.device.family == "Other" then "Computer" else parsed.device.family
                    ),
                    td(parts(parsed.os.family.some, parsed.os.major)),
                    td(parts(parsed.userAgent.family.some, parsed.userAgent.major)),
                    td(dataSort := date.toMillis)(momentFromNowServer(date)),
                    td(UserClient(ua).toString)
                  )
                }
            )
          )
        )
      ),
      div(id := "identification_screen", cls := "spy_ips")(
        table(cls := "slist spy_filter slist--sort")(
          thead(
            tr(
              th(pluralize("IP", logins.ips.size)),
              thSortNumber("Alts"),
              th,
              th("Client"),
              thSortNumber("Date"),
              canIpBan.option(thSortNumber)
            )
          ),
          tbody(
            logins.ips.sortBy(ip => (-ip.alts.score, -ip.ip.seconds)).map { ip =>
              val renderedIp = renderIp(ip.ip.value)
              tr(cls := ip.blocked.option("blocked"), dataValue := renderedIp, dataTags := ip.location.id)(
                td(a(href := routes.Mod.singleIp(renderedIp))(renderedIp)),
                td(dataSort := ip.alts.score)(altMarks(ip.alts)),
                td(ip.proxy.name.map { proxy => span(cls := "proxy", title := "Proxy")(proxy) }),
                td(ip.clients.toList.map(_.toString).sorted.mkString(", ")),
                td(dataSort := ip.ip.date.toMillis)(momentFromNowServer(ip.ip.date)),
                canIpBan.option(
                  td(dataSort := (9999 - ip.alts.cleans))(
                    button(
                      cls := List(
                        "button button-empty" -> true,
                        "button-discouraging" -> (ip.alts.cleans > 0 || othersPartiallyLoaded)
                      ),
                      href := routes.Mod.singleIpBan(!ip.blocked, ip.ip.value.value)
                    )("BAN")
                  )
                )
              )
            }
          )
        )
      ),
      div(cls := "spy_fps")(
        table(cls := "slist spy_filter slist--sort")(
          thead(
            tr(
              th(pluralize("Print", logins.prints.size)),
              thSortNumber("Alts"),
              th("Client"),
              thSortNumber("Date"),
              canFpBan.option(thSortNumber)
            )
          ),
          tbody(
            logins.prints.sortBy(fp => (-fp.alts.score, -fp.fp.seconds)).map { fp =>
              tr(cls := fp.banned.option("blocked"), dataValue := fp.fp.value)(
                td(a(href := routes.Mod.print(fp.fp.value.value))(fp.fp.value)),
                td(dataSort := fp.alts.score)(altMarks(fp.alts)),
                td(fp.client.toString),
                td(dataSort := fp.fp.date.toMillis)(momentFromNowServer(fp.fp.date)),
                canFpBan.option(
                  td(dataSort := (9999 - fp.alts.cleans))(
                    button(
                      cls := List(
                        "button button-empty" -> true,
                        "button-discouraging" -> (fp.alts.cleans > 0 || othersPartiallyLoaded)
                      ),
                      href := routes.Mod.printBan(!fp.banned, fp.fp.value.value)
                    )("BAN")
                  )
                )
              )
            }
          )
        )
      )
    )

  private def altMarks(alts: UserLogins.Alts) =
    List[(Int, Frag)](
      alts.boosters -> boosting,
      alts.engines -> engine,
      alts.trolls -> shadowban,
      alts.alts -> alt,
      alts.closed -> closed,
      alts.cleans -> clean
    ).collect:
      case (nb, tag) if nb > 4 => frag(List.fill(3)(tag), "+", nb - 3)
      case (nb, tag) if nb > 0 => frag(List.fill(nb)(tag))
