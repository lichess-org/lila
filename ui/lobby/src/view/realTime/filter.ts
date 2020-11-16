import { h } from "snabbdom";
import { bind } from "../util";
import LobbyController from "../../ctrl";

function initialize(ctrl: LobbyController, el: HTMLElement) {
  const f = ctrl.filter.data?.form,
    $div = $(el),
    $ratingRange = $div.find(".rating-range");

  if (f)
    Object.keys(f).forEach((k) => {
      const input = $div.find(`input[name="${k}"]`)[0] as HTMLInputElement;
      if (input.type == "checkbox") input.checked = true;
      else input.value = f[k];
    });
  else $div.find("input").prop("checked", true);

  const save = () => ctrl.filter.save($div.find("form")[0] as HTMLFormElement);

  function changeRatingRange(values) {
    $ratingRange.find("input").val(values[0] + "-" + values[1]);
    $ratingRange.siblings(".range").text(values[0] + "–" + values[1]);
    save();
  }
  $div.find("input").change(save);
  $div.find("button.reset").click(function () {
    ctrl.filter.set(null);
    ctrl.filter.open = false;
    ctrl.redraw();
  });
  $div.find("button.apply").click(function () {
    ctrl.filter.open = false;
    ctrl.redraw();
  });
  $ratingRange.each(function (this: HTMLElement) {
    var $this = $(this);
    window.lishogi.slider().done(function () {
      var $input = $this.find("input");
      var $span = $this.siblings(".range");
      var min = $input.data("min");
      var max = $input.data("max");
      var values = $input.val() ? $input.val().split("-") : [min, max];
      $span.text(values.join("–"));
      $this.slider({
        range: true,
        min: min,
        max: max,
        values: values,
        step: 50,
        slide(_, ui) {
          changeRatingRange(ui.values);
        },
      });
    });
  });
}

export function toggle(ctrl: LobbyController, nbFiltered: number) {
  const filter = ctrl.filter;
  return h("i.toggle.toggle-filter", {
    class: { gamesFiltered: nbFiltered > 0, active: filter.open },
    hook: bind("mousedown", filter.toggle, ctrl.redraw),
    attrs: {
      "data-icon": filter.open ? "L" : "%",
      title: ctrl.trans.noarg("filterGames"),
    },
  });
}

export interface FilterNode extends HTMLElement {
  filterLoaded?: boolean;
}

export function render(ctrl: LobbyController) {
  return h("div.hook__filters", {
    hook: {
      insert(vnode) {
        const el = vnode.elm as FilterNode;
        if (el.filterLoaded) return;
        $.ajax({
          url: "/setup/filter",
          success(html) {
            el.innerHTML = html;
            el.filterLoaded = true;
            initialize(ctrl, el);
          },
        });
      },
    },
  });
}
