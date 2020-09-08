import { FormStore, toFormLines, makeStore } from './form';
import modal from 'common/modal';
import debounce from 'common/debounce';
import * as xhr from 'common/xhr';
import LobbyController from './ctrl';

const li = window.lichess;

export default class Setup {

  stores: {
    hook: FormStore;
    friend: FormStore;
    ai: FormStore;
  }

  ratingRange = () => this.stores.hook.get()?.ratingRange;

  constructor(makeStorage: (name: string) => LichessStorage, readonly root: LobbyController) {
    this.stores = {
      hook: makeStore(makeStorage('lobby.setup.hook')),
      friend: makeStore(makeStorage('lobby.setup.friend')),
      ai: makeStore(makeStorage('lobby.setup.ai'))
    };
  }

  private save = (form: HTMLFormElement) => {
    this.stores[form.getAttribute('data-type')!].set(toFormLines(form));
  }

  private sliderTimes = [
    0, 1 / 4, 1 / 2, 3 / 4, 1, 3 / 2, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16,
    17, 18, 19, 20, 25, 30, 35, 40, 45, 60, 75, 90, 105, 120, 135, 150, 165, 180
  ];

  private sliderTime = (v: number) => (v < this.sliderTimes.length ? this.sliderTimes[v] : 180);

  private sliderIncrement = (v: number) => {
    if (v <= 20) return v;
    switch (v) {
      case 21:
        return 25;
      case 22:
        return 30;
      case 23:
        return 35;
      case 24:
        return 40;
      case 25:
        return 45;
      case 26:
        return 60;
      case 27:
        return 90;
      case 28:
        return 120;
      case 29:
        return 150;
      default:
        return 180;
    }
  }

  private sliderDays = (v: number) => {
    if (v <= 3) return v;
    switch (v) {
      case 4:
        return 5;
      case 5:
        return 7;
      case 6:
        return 10;
      default:
        return 14;
    }
  }

  private sliderInitVal = (v, f, max) => {
    for (let i = 0; i < max; i++) {
      if (f(i) === v) return i;
    }
    return undefined;
  }

  private hookToPoolMember = (color: string, form: HTMLFormElement) => {
    const data = Array.from(new FormData(form).entries());
    const hash: any = {};
    for (let i in data) hash[data[i][0]] = data[i][1];
    const valid = color == 'random' && hash.variant == 1 && hash.mode == 1 && hash.timeMode == 1,
      id = parseFloat(hash.time) + '+' + parseInt(hash.increment);
    return (valid && this.root.pools.find(p => p.id === id)) ? {
      id,
      range: hash.ratingRange
    } : undefined;
  }

  prepareForm = ($modal: JQuery) => {
    const self = this,
      $form = $modal.find('form'),
      $timeModeSelect = $form.find('#sf_timeMode'),
      $modeChoicesWrap = $form.find('.mode_choice'),
      $modeChoices = $modeChoicesWrap.find('input'),
      $casual = $modeChoices.eq(0),
      $rated = $modeChoices.eq(1),
      $variantSelect = $form.find('#sf_variant'),
      $fenPosition = $form.find(".fen_position"),
      $fenInput = $fenPosition.find('input'),
      forceFormPosition = !!$fenInput.val(),
      $timeInput = $form.find('.time_choice [name=time]'),
      $incrementInput = $form.find('.increment_choice [name=increment]'),
      $daysInput = $form.find('.days_choice [name=days]'),
      typ = $form.data('type'),
      $ratings = $modal.find('.ratings > div'),
      randomColorVariants = $form.data('random-color-variants').split(','),
      $submits = $form.find('.color-submits__button'),
      toggleButtons = function() {
        const variantId = $variantSelect.val(),
          timeMode = $timeModeSelect.val(),
          rated = $rated.prop('checked'),
          limit = $timeInput.val(),
          inc = $incrementInput.val(),
          // no rated variants with less than 30s on the clock
          cantBeRated = (timeMode == '1' && variantId != '1' && limit < 0.5 && inc == 0) ||
            (variantId != '1' && timeMode != '1');
        if (cantBeRated && rated) {
          $casual.click();
          return toggleButtons();
        }
        $rated.prop('disabled', !!cantBeRated).siblings('label').toggleClass('disabled', cantBeRated);
        const timeOk = timeMode != '1' || limit > 0 || inc > 0,
          ratedOk = typ != 'hook' || !rated || timeMode != '0',
          aiOk = typ != 'ai' || variantId != '3' || limit >= 1;
        if (timeOk && ratedOk && aiOk) {
          $submits.toggleClass('nope', false);
          $submits.filter(':not(.random)').toggle(!rated || !randomColorVariants.includes(variantId));
        } else $submits.toggleClass('nope', true);
      },
      save = function() {
        self.save($form[0] as HTMLFormElement);
      };

    const c = this.stores[typ].get();
    if (c) {
      Object.keys(c).forEach(k => {
        $form[0].querySelectorAll(`[name="${k}"]`).forEach((input: HTMLInputElement) => {
          if (input.type == 'checkbox') input.checked = true;
          else if (input.type == 'radio') input.checked = input.value == c[k];
          else if (k != 'fen' || !input.value) input.value = c[k];
        });
      });
    }

    const showRating = () => {
      const timeMode = $timeModeSelect.val();
      let key;
      switch ($variantSelect.val()) {
        case '1':
        case '3':
          if (timeMode == '1') {
            const time = $timeInput.val() * 60 + $incrementInput.val() * 40;
            if (time < 30) key = 'ultraBullet';
            else if (time < 180) key = 'bullet';
            else if (time < 480) key = 'blitz';
            else if (time < 1500) key = 'rapid';
            else key = 'classical';
          } else key = 'correspondence';
          break;
        case '10':
          key = 'crazyhouse';
          break;
        case '2':
          key = 'chess960';
          break;
        case '4':
          key = 'kingOfTheHill';
          break;
        case '5':
          key = 'threeCheck';
          break;
        case '6':
          key = 'antichess'
          break;
        case '7':
          key = 'atomic'
          break;
        case '8':
          key = "horde"
          break;
        case '9':
          key = "racingKings"
          break;
      }
      $ratings.hide().filter('.' + key).show();
      save();
    };
    if (typ == 'hook') {
      if ($form.data('anon')) {
        $timeModeSelect.val(1)
          .children('.timeMode_2, .timeMode_0')
          .prop('disabled', true)
          .attr('title', this.root.trans('youNeedAnAccountToDoThat'));
      }
      const ajaxSubmit = color => {
        const poolMember = this.hookToPoolMember(color, $form[0] as HTMLFormElement);
        modal.close();
        if (poolMember) {
          this.root.enterPool(poolMember);
          this.root.redraw();
        } else {
          this.root.setTab($timeModeSelect.val() === '1' ? 'real_time' : 'seeks');
          xhr.text(
            $form.attr('action').replace(/sri-placeholder/, li.sri),
            {
              method: 'post',
              body: (() => {
                const data = new FormData($form[0] as HTMLFormElement)
                data.append('color', color);
                console.log(data);
                return data;
              })()
            });
        }
        return false;
      };
      $submits.click(function(this: HTMLElement) {
        return ajaxSubmit($(this).val());
      }).prop('disabled', false);
      $form.submit(function() {
        return ajaxSubmit('random');
      });
    } else $form.one('submit', function() {
      $submits.hide().end().append(li.spinnerHtml);
    });
    if (this.root.opts.blindMode) {
      $variantSelect.focus();
      $timeInput.add($incrementInput).on('change', function() {
        toggleButtons();
        showRating();
      });
    } else li.slider().then(() => {
      $timeInput.add($incrementInput).each(function(this: HTMLElement) {
        const $input = $(this),
          $value = $input.siblings('span'),
          isTimeSlider = $input.parent().hasClass('time_choice'),
          showTime = (v: number) => {
            if (v == 1 / 4) return '¼';
            if (v == 1 / 2) return '½';
            if (v == 3 / 4) return '¾';
            return v;
          },
          valueToTime = (v: number) => (isTimeSlider ? self.sliderTime : self.sliderIncrement)(v),
          show = (time: number) => $value.text(isTimeSlider ? showTime(time) : time);
        show(parseFloat($input.val()));
        $input.after($('<div>').slider({
          value: self.sliderInitVal(parseFloat($input.val()), isTimeSlider ? self.sliderTime : self.sliderIncrement, 100),
          min: 0,
          max: isTimeSlider ? 38 : 30,
          range: 'min',
          step: 1,
          slide: function(_, ui) {
            const time = valueToTime(ui.value);
            show(time);
            $input.val(time);
            showRating();
            toggleButtons();
          }
        }));
      });
      $daysInput.each(function(this: HTMLElement) {
        var $input = $(this),
          $value = $input.siblings('span');
        $value.text($input.val());
        $input.after($('<div>').slider({
          value: self.sliderInitVal(parseInt($input.val()), self.sliderDays, 20),
          min: 1,
          max: 7,
          range: 'min',
          step: 1,
          slide: function(_, ui) {
            const days = self.sliderDays(ui.value);
            $value.text(days);
            $input.attr('value', days);
            save();
          }
        }));
      });
      $form.find('.rating-range').each(function(this: HTMLElement) {
        const $this = $(this),
          $input = $this.find("input"),
          $span = $this.siblings("span.range"),
          min = $input.data("min"),
          max = $input.data("max"),
          values = $input.val() ? $input.val().split("-") : [min, max];

        $span.text(values.join('–'));
        $this.slider({
          range: true,
          min: min,
          max: max,
          values: values,
          step: 50,
          slide: function(_, ui) {
            $input.val(ui.values[0] + "-" + ui.values[1]);
            $span.text(ui.values[0] + "–" + ui.values[1]);
            save();
          }
        });
      });
    });
    $timeModeSelect.on('change', function(this: HTMLElement) {
      var timeMode = $(this).val();
      $form.find('.time_choice, .increment_choice').toggle(timeMode == '1');
      $form.find('.days_choice').toggle(timeMode == '2');
      toggleButtons();
      showRating();
    }).trigger('change');

    var validateFen = debounce(() => {
      $fenInput.removeClass("success failure");
      var fen = $fenInput.val();
      if (fen) xhr.text(xhr.url($fenInput.parent().data('validate-url'), { fen }))
        .then(data => {
          $fenInput.addClass("success");
          $fenPosition.find('.preview').html(data);
          $fenPosition.find('a.board_editor').each(function(this: HTMLElement) {
            $(this).attr('href', $(this).attr('href').replace(/editor\/.+$/, "editor/" + fen));
          });
          $submits.removeClass('nope');
          li.pubsub.emit('content_loaded');
        })
        .catch(() => {
          $fenInput.addClass("failure");
          $fenPosition.find('.preview').html("");
          $submits.addClass('nope');
        })
    }, 200);
    $fenInput.on('keyup', validateFen);

    if (forceFormPosition) $variantSelect.val(3);
    $variantSelect.on('change', function(this: HTMLElement) {
      var isFen = $(this).val() == '3';
      $fenPosition.toggle(isFen);
      $modeChoicesWrap.toggle(!isFen);
      if (isFen) {
        $casual.click();
        requestAnimationFrame(() => document.body.dispatchEvent(new Event('chessground.resize')));
      }
      showRating();
      toggleButtons();
    }).trigger('change');

    $modeChoices.on('change', save);

    $form.find('div.level').each(function(this: HTMLElement) {
      var $infos = $(this).find('.ai_info > div');
      $(this).find('label').on('mouseenter', function(this: HTMLElement) {
        $infos.hide().filter('.' + $(this).attr('for')).show();
      });
      $(this).find('#config_level').on('mouseleave', function(this: HTMLElement) {
        var level = $(this).find('input:checked').val();
        $infos.hide().filter('.sf_level_' + level).show();
      }).trigger('mouseout');
      $(this).find('input').on('change', save);
    });
  }
}
