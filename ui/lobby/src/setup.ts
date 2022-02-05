import { FormStore, toFormLines, makeStore } from './form';
import LobbyController from './ctrl';

const li = window.lishogi;

export default class Setup {
  stores: {
    hook: FormStore;
    friend: FormStore;
    ai: FormStore;
  };

  ratingRange = () => this.stores.hook.get()?.ratingRange;

  constructor(makeStorage: (name: string) => LishogiStorage, readonly root: LobbyController) {
    this.stores = {
      hook: makeStore(makeStorage('lobby.setup.hook')),
      friend: makeStore(makeStorage('lobby.setup.friend')),
      ai: makeStore(makeStorage('lobby.setup.ai')),
    };
  }

  private save = (form: HTMLFormElement) => {
    this.stores[form.getAttribute('data-type')!].set(toFormLines(form));
  };

  private sliderTimes = [
    0,
    1 / 4,
    1 / 2,
    3 / 4,
    1,
    3 / 2,
    2,
    3,
    4,
    5,
    6,
    7,
    8,
    9,
    10,
    11,
    12,
    13,
    14,
    15,
    16,
    17,
    18,
    19,
    20,
    25,
    30,
    35,
    40,
    45,
    60,
    75,
    90,
    105,
    120,
    135,
    150,
    165,
    180,
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
  };

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
  };

  private sliderInitVal = (v, f, max) => {
    for (let i = 0; i < max; i++) {
      if (f(i) === v) return i;
    }
    return undefined;
  };

  private hookToPoolMember = (color, data) => {
    const hash: any = {};
    for (var i in data) hash[data[i].name] = data[i].value;
    const valid = color == 'random' && hash.variant == 1 && hash.mode == 1 && hash.timeMode == 1,
      id = parseFloat(hash.time) + '+' + parseInt(hash.increment);
    return valid && this.root.pools.find(p => p.id === id)
      ? {
          id: id,
          range: hash.ratingRange,
        }
      : undefined;
  };

  prepareForm = ($modal: JQuery) => {
    const self = this,
      $form = $modal.find('form'),
      $timeModeSelect = $form.find('#sf_timeMode'),
      $modeChoicesWrap = $form.find('.mode_choice'),
      $modeChoices = $modeChoicesWrap.find('input'),
      $casual = $modeChoices.eq(0),
      $rated = $modeChoices.eq(1),
      $variantSelect = $form.find('#sf_variant'),
      $sfenPosition = $form.find('.sfen_position'),
      $sfenInput = $sfenPosition.find('input'),
      $handicapSelect = $sfenPosition.find('.handicap select'),
      forceFromPosition = !!$sfenInput.val(),
      $timeInput = $form.find('.time_choice [name=time]'),
      $incrementInput = $form.find('.increment_choice [name=increment]'),
      $byoyomiInput = $form.find('.byoyomi_choice [name=byoyomi]'),
      $periodsInput = $form.find('.periods [name=periods]'),
      $advancedSetup = $form.find('.advanced_setup'),
      $advancedToggle = $form.find('.advanced_toggle'),
      $daysInput = $form.find('.days_choice [name=days]'),
      typ = $form.data('type'),
      $ratings = $modal.find('.ratings > div'),
      $submits = $form.find('.color-submits__button'),
      $submitsError = $form.find('.submit-error-message'),
      toggleButtons = function () {
        const variantId = $variantSelect.val(),
          timeMode = $timeModeSelect.val(),
          rated = $rated.prop('checked'),
          limit = $timeInput.val(),
          inc = $incrementInput.val(),
          byo = $byoyomiInput.val(),
          per = $periodsInput.filter(':checked').val(),
          cantBeRated =
            (typ === 'hook' && timeMode === '0') ||
            (variantId != '1' && timeMode != '1') ||
            (timeMode == '1' && (per > 1 || (inc > 0 && byo > 0)));
        if (cantBeRated && rated) {
          $casual.click();
          return toggleButtons();
        }
        $rated.prop('disabled', !!cantBeRated).siblings('label').toggleClass('disabled', cantBeRated);
        const timeOk = timeMode != '1' || ((limit > 0 || inc > 0 || byo > 0) && (byo || per == 1)),
          ratedOk = typ != 'hook' || !rated || timeMode != '0',
          aiOk = typ != 'ai' || variantId == '1' || limit >= 1 || byo >= 10 || inc >= 5;
        if (timeOk && ratedOk && aiOk) {
          $submits.toggleClass('nope', false);
          $submitsError.html('');
          $submits.filter(':not(.random)').toggle(!rated);
        } else {
          $submits.toggleClass('nope', true);
          $submitsError.html('Invalid time control!');
        }

        if (byo > 0) $('.periods').show();
        else $('.periods').hide();
      },
      save = function () {
        self.save($form[0] as HTMLFormElement);
      },
      displayAdvanced = function () {
        if (($incrementInput.val() == 0 && $periodsInput.filter(':checked').val() == 1) || $timeModeSelect.val() != 1) {
          $advancedToggle.attr('data-icon', 'R');
          $advancedSetup.hide();
          $advancedSetup.addClass('hidden');
        } else {
          $advancedToggle.attr('data-icon', 'S');
          $advancedSetup.show();
          $advancedSetup.removeClass('hidden');
        }
      },
      // displays properly only for value < 20 or smth - slider increment
      updateSlider = function (val) {
        $incrementInput.val(val);
        $('.increment_choice .ui-slider').slider('value', val);
        $('.increment_choice input').siblings('span').text(val);
      },
      updatePeriods = function (val) {
        $('.periods #sf_periods_' + val).click();
      },
      resetPeriods = function () {
        if ($byoyomiInput.val() == 0) updatePeriods(1);
      };
    const c = this.stores[typ].get();
    if (c) {
      Object.keys(c).forEach(k => {
        $form[0].querySelectorAll(`[name="${k}"]`).forEach((input: HTMLInputElement) => {
          if (k === 'timeMode' && input.value !== '1') return;
          if (input.type == 'checkbox') input.checked = true;
          else if (input.type == 'radio') input.checked = input.value == c[k];
          else if (k != 'sfen' || !input.value) input.value = c[k];
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
            const time =
              $timeInput.val() * 60 +
              $incrementInput.val() * 60 +
              $byoyomiInput.val() * 25 * $periodsInput.filter(':checked').val();
            if (time < 60) key = 'ultraBullet';
            else if (time < 300) key = 'bullet';
            else if (time < 599) key = 'blitz';
            else if (time < 1500) key = 'rapid';
            else key = 'classical';
          } else key = 'correspondence';
          break;
        case '2':
          key = 'minishogi';
          break;
      }
      $ratings
        .hide()
        .filter('.' + key)
        .show();
      save();
    };
    if (typ == 'hook') {
      if ($form.data('anon')) {
        $timeModeSelect
          .val(1)
          .children('.timeMode_2, .timeMode_0')
          .prop('disabled', true)
          .attr('title', this.root.trans('youNeedAnAccountToDoThat'));
      }
      const ajaxSubmit = color => {
        const poolMember = this.hookToPoolMember(color, $form.serializeArray());
        $.modal.close();
        if (poolMember) {
          this.root.enterPool(poolMember);
        } else {
          this.root.setTab($timeModeSelect.val() === '1' ? 'real_time' : 'seeks');
          $.ajax({
            url: $form.attr('action').replace(/sri-placeholder/, li.sri),
            data: $form.serialize() + '&color=' + color,
            type: 'post',
          });
        }
        this.root.redraw();
        return false;
      };
      $submits
        .click(function (this: HTMLElement) {
          return ajaxSubmit($(this).val());
        })
        .prop('disabled', false);
      $form.submit(function () {
        return ajaxSubmit('random');
      });
    } else
      $form.one('submit', function () {
        $submits.hide().end().append(li.spinnerHtml);
      });
    $timeInput.add($periodsInput).on('change', function () {
      toggleButtons();
      showRating();
    });
    if (this.root.opts.blindMode) {
      $variantSelect.focus();
      $timeInput.add($incrementInput).on('change', function () {
        toggleButtons();
        showRating();
      });
      $timeInput.add($byoyomiInput).on('change', function () {
        toggleButtons();
        showRating();
      });
    } else
      li.slider().done(function () {
        $timeInput
          .add($incrementInput)
          .add($byoyomiInput)
          .each(function (this: HTMLElement) {
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
            $input.after(
              $('<div>').slider({
                value: self.sliderInitVal(
                  parseFloat($input.val()),
                  isTimeSlider ? self.sliderTime : self.sliderIncrement,
                  100
                ),
                min: 0,
                max: isTimeSlider ? 38 : 30,
                range: 'min',
                step: 1,
                slide: function (_, ui) {
                  const time = valueToTime(ui.value);
                  show(time);
                  $input.val(time);
                  showRating();
                  resetPeriods();
                  toggleButtons();
                },
              })
            );
          });
        $daysInput.each(function (this: HTMLElement) {
          var $input = $(this),
            $value = $input.siblings('span');
          $value.text($input.val());
          $input.after(
            $('<div>').slider({
              value: self.sliderInitVal(parseInt($input.val()), self.sliderDays, 20),
              min: 1,
              max: 7,
              range: 'min',
              step: 1,
              slide: function (_, ui) {
                const days = self.sliderDays(ui.value);
                $value.text(days);
                $input.attr('value', days);
                save();
              },
            })
          );
        });
        $form.find('.rating-range').each(function (this: HTMLElement) {
          const $this = $(this),
            $input = $this.find('input'),
            $span = $this.siblings('span.range'),
            min = $input.data('min'),
            max = $input.data('max'),
            values = $input.val() ? $input.val().split('-') : [min, max];

          $span.text(values.join('–'));
          $this.slider({
            range: true,
            min: min,
            max: max,
            values: values,
            step: 50,
            slide: function (_, ui) {
              $input.val(ui.values[0] + '-' + ui.values[1]);
              $span.text(ui.values[0] + '–' + ui.values[1]);
              save();
            },
          });
        });
      });
    $timeModeSelect
      .on('change', function (this: HTMLElement) {
        var timeMode = $(this).val();
        $form.find('.time_choice, .byoyomi_choice, .advanced_toggle').toggle(timeMode == '1');
        $form.find('.days_choice').toggle(timeMode == '2');
        if (timeMode == '1') displayAdvanced();
        if (timeMode == '2') $advancedSetup.hide();
        toggleButtons();
        showRating();
      })
      .trigger('change');

    var validateSfen = li.debounce(function () {
      $sfenInput.removeClass('success failure');
      const sfen = $sfenInput.val();
      const variant = $variantSelect.val();
      if (sfen) {
        $.ajax({
          url: $sfenInput.parent().data('validate-url'),
          data: {
            sfen: sfen,
            variant: variant,
          },
          success: function (data) {
            $sfenInput.addClass('success');
            $sfenPosition.find('.preview').html(data);
            $sfenPosition.find('a.board_editor').each(function (this: HTMLElement) {
              $(this).attr(
                'href',
                $(this)
                  .attr('href')
                  .replace(/editor\/.+$/, 'editor/' + sfen)
              );
            });
            $submits.removeClass('nope');
            $submitsError.html('');
            li.pubsub.emit('content_loaded');
          },
          error: function () {
            $sfenInput.addClass('failure');
            $sfenPosition.find('.preview').html('');
            $submits.addClass('nope');
            $submitsError.html('Invalid sfen!');
          },
        });
      }
    }, 200);

    var validateSfenWrapper = function (changeHandicapSelect) {
      return function () {
        if (changeHandicapSelect) $handicapSelect.val('');
        validateSfen();
      };
    };
    $sfenInput.on('keyup', validateSfenWrapper(true));

    var setHandicap = function () {
      const hcSfen = $handicapSelect.val();
      if (hcSfen) {
        $sfenInput.val($handicapSelect.val());
        validateSfenWrapper(false)();
      }
    };
    $handicapSelect.on('change', setHandicap);

    if (forceFromPosition) $variantSelect.val(3);
    $variantSelect
      .on('change', function (this: HTMLElement) {
        var isSfen = $(this).val() == '3';
        $sfenPosition.toggle(isSfen);
        $modeChoicesWrap.toggle(!isSfen);
        if (isSfen) {
          $casual.click();
          requestAnimationFrame(() => li.dispatchEvent(document.body, 'shogiground.resize'));
        }
        showRating();
        toggleButtons();
      })
      .trigger('change');

    $modeChoices.on('change', save);

    // We hide the advanced menu only if the user isn't using it
    displayAdvanced();

    $advancedToggle.click(() => {
      if ($advancedSetup.hasClass('hidden')) {
        $advancedSetup.show();
        $advancedSetup.removeClass('hidden');
        $advancedToggle.attr('data-icon', 'S');
      } else {
        $advancedSetup.hide();
        $advancedSetup.addClass('hidden');
        $advancedToggle.attr('data-icon', 'R');
        updateSlider('0');
        updatePeriods('1');
        toggleButtons();
      }
    });

    $form.find('div.level').each(function (this: HTMLElement) {
      var $infos = $(this).find('.ai_info > div');
      $(this)
        .find('label')
        .on('mouseenter', function (this: HTMLElement) {
          $infos
            .hide()
            .filter('.' + $(this).attr('for'))
            .show();
        });
      $(this)
        .find('#config_level')
        .on('mouseleave', function (this: HTMLElement) {
          var level = $(this).find('input:checked').val();
          $infos
            .hide()
            .filter('.sf_level_' + level)
            .show();
        })
        .trigger('mouseout');
      $(this).find('input').on('change', save);
    });
  };
}
