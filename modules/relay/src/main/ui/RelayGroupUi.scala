package lila.relay
package ui

import lila.ui.*
import ScalatagsTemplate.{ *, given }

final class RelayGroupUi(ui: RelayUi, card: RelayCardUi, pageMenu: RelayMenuUi):
  def show(
      group: lila.relay.RelayGroup,
      tours: List[lila.relay.RelayTour]
  )(using Context) =
    Page(group.name.value).css("bits.relay.group"):
      main(cls := "relay-group page-menu")(
        pageMenu("index"),
        div(cls := "page__content box box-pad page")(
          boxTop(ui.broadcastH1(group.name)),
          div(cls := "relay-group__tours relay-cards")(
            tours.map(card.renderTourOfGroup(group))
          )
        )
      )
