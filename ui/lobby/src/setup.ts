import { colorName } from 'common/colorName';
import { engineName } from 'common/engineName';
import { findHandicaps, isHandicap } from 'shogiops/handicaps';
import { initialSfen } from 'shogiops/sfen';
import LobbyController from './ctrl';
import { FormStore, makeStore, toFormLines } from './form';
import { clockEstimateSeconds } from 'common/clock';

const li = window.lishogi;

export default class Setup {
  stores: {
    hook: FormStore;
    friend: FormStore;
    ai: FormStore;
  };

  ratingRange = () => this.stores.hook.get()?.ratingRange;

  constructor(
    makeStorage: (name: string) => LishogiStorage,
    readonly root: LobbyController
  ) {
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

  private ratingKey = (variantId: string, realTime: boolean, timeSum: number): string => {
    switch (variantId) {
      case '2':
        return 'minishogi';
      case '3':
        return 'chushogi';
      case '4':
        return 'annanshogi';
      case '5':
        return 'kyotoshogi';
      case '6':
        return 'checkshogi';
      default:
        if (realTime) {
          if (timeSum < 60) return 'ultraBullet';
          else if (timeSum < 300) return 'bullet';
          else if (timeSum < 599) return 'blitz';
          else if (timeSum < 1500) return 'rapid';
          else return 'classical';
        } else return 'correspondence';
    }
  };

  private idToRules = (id: number | string): VariantKey => {
    switch (typeof id === 'string' ? parseInt(id) : id) {
      case 2:
        return 'minishogi';
      case 3:
        return 'chushogi';
      case 4:
        return 'annanshogi';
      case 5:
        return 'kyotoshogi';
      case 6:
        return 'checkshogi';
      default:
        return 'standard';
    }
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
      $position = $form.find('.sfen_position'),
      $positionInput = $position.find('.radio [name=position]'),
      $default = $positionInput.eq(0),
      $fromPosition = $positionInput.eq(1),
      $positionWrap = $position.find('.sfen_position_wrap'),
      $sfenInput = $positionWrap.find('input'),
      $handicap = $positionWrap.find('.handicap'),
      $handicapSelect = $handicap.find('select'),
      $positionPreview = $positionWrap.find('.preview'),
      $timeInput = $form.find('.time_choice [name=time]'),
      $incrementInput = $form.find('.increment_choice [name=increment]'),
      $byoyomiInput = $form.find('.byoyomi_choice [name=byoyomi]'),
      $periods = $form.find('.periods'),
      $periodsInput = $periods.find('.periods [name=periods]'),
      $advancedTimeSetup = $form.find('.advanced_setup'),
      $advancedTimeToggle = $form.find('.advanced_toggle'),
      $daysInput = $form.find('.days_choice [name=days]'),
      typ = $form.data('type'),
      $ratings = $modal.find('.ratings > div'),
      $submits = $form.find('.color-submits__button');

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

    const toggleButtons = (): void => {
      const variantId = $variantSelect.val(),
        timeMode = $timeModeSelect.val(),
        limit = $timeInput.val(),
        inc = $incrementInput.val(),
        byo = $byoyomiInput.val(),
        per = $periodsInput.filter(':checked').val(),
        hasSfen = !!$sfenInput.val(),
        cantBeRated =
          hasSfen ||
          (typ === 'hook' && timeMode === '0') ||
          (timeMode == '1' && (per > 1 || (inc > 0 && byo > 0))) ||
          (variantId == '3' &&
            clockEstimateSeconds(parseInt(limit) * 60, parseInt(byo), parseInt(inc), parseInt(per)) < 250);

      $periods.toggle(byo > 0);

      if (cantBeRated && $rated.prop('checked')) $casual.click();
      $rated.prop('disabled', !!cantBeRated).siblings('label').toggleClass('disabled', cantBeRated);

      const timeOk = timeMode !== '1' || ((limit > 0 || inc > 0 || byo > 0) && (byo || per === 1)),
        ratedOk = typ !== 'hook' || timeMode !== '0' || !$rated.prop('checked'),
        aiOk = typ !== 'ai' || variantId === '1' || limit >= 1 || byo >= 10 || inc >= 5;

      if (timeOk)
        $ratings
          .hide()
          .filter('.' + this.ratingKey(variantId, timeMode === '1', limit * 60 + inc * 60 + byo * 25 * per))
          .show();
      else $ratings.hide;

      if (timeOk && ratedOk && aiOk) $submits.prop('disabled', false);
      else $submits.prop('disabled', true);
    };

    const save = (): void => {
      self.save($form[0] as HTMLFormElement);
    };

    const resetIncSlider = (): void => {
      $incrementInput.val('0');
      $('.increment_choice .ui-slider').slider('value', '0');
      $('.increment_choice input').siblings('span').text('0');
    };

    const resetPeriods = (): void => {
      $periodsInput.eq(0).click();
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
        $.modal.close();
        this.root.setTab($timeModeSelect.val() === '1' ? 'real_time' : 'seeks');
        $.ajax({
          url: $form.attr('action').replace(/sri-placeholder/, li.sri),
          data: $form.serialize() + '&color=' + color,
          type: 'post',
        });
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
      save();
    });
    if (this.root.opts.blindMode) {
      $variantSelect.focus();
      $timeInput.add($incrementInput).on('change', function () {
        toggleButtons();
        save();
      });
      $timeInput.add($byoyomiInput).on('change', function () {
        toggleButtons();
        save();
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
                  if ($byoyomiInput.val() === '0' && $periodsInput.filter(':checked').val() !== '1') resetPeriods();
                  toggleButtons();
                  save();
                },
              })
            );
          });
        $daysInput.each(function (this: HTMLElement) {
          const $input = $(this),
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

    const initAdvancedTimeSetup = (): void => {
      $advancedTimeToggle.toggleClass('show-inline-block', $timeModeSelect.val() === '1');
      if (
        ($incrementInput.val() === '0' && $periodsInput.filter(':checked').val() === '1') ||
        $timeModeSelect.val() !== '1'
      ) {
        $advancedTimeSetup.hide();
        $advancedTimeToggle.removeClass('active');
      } else {
        $advancedTimeSetup.show();
        $advancedTimeToggle.addClass('active');
      }
    };

    $timeModeSelect
      .on('change', function (this: HTMLElement) {
        const timeMode = $(this).val();
        $form
          .find('.time_choice, .byoyomi_choice, .periods, .increment_choice, .advanced_toggle')
          .toggle(timeMode === '1');
        $form.find('.days_choice').toggle(timeMode === '2');
        initAdvancedTimeSetup();
        toggleButtons();
        save();
      })
      .trigger('change');

    const updateHandicaps = () => {
      const buildOption = (value: string, name: string): string => {
        return `<option value="${value}">${name}</option>`;
      };
      const rules = this.idToRules($variantSelect.val()),
        handicaps = findHandicaps({ rules }),
        options = handicaps
          .map(h => {
            return buildOption(h.sfen, `${h.japaneseName} (${h.englishName})`);
          })
          .join(''),
        defaultOption = buildOption(initialSfen(rules), this.root.trans.noarg('startPosition'));
      $handicapSelect.html(defaultOption + options);
    };

    const updateEngineName = () => {
      const sfen = $sfenInput.val(),
        variant = $variantSelect.val(),
        $level = $('div.level'),
        $info = $level.find('.ai_info'),
        rules = this.idToRules(variant),
        level = parseInt($level.find('input:checked').val());
      $form
        .find('.color-submits button[value="sente"]')
        .attr('title', colorName(this.root.trans.noarg, 'sente', isHandicap({ sfen, rules })));
      $form
        .find('.color-submits button[value="gote"]')
        .attr('title', colorName(this.root.trans.noarg, 'gote', isHandicap({ sfen, rules })));

      $info.text(engineName(rules, sfen, level));
    };

    const setHandicapvalue = () => {
      const sfen = $sfenInput.val();
      $handicapSelect.val(sfen || initialSfen(this.idToRules($variantSelect.val())));
    };

    const validateSfen = li.debounce(function () {
      $sfenInput.removeClass('success failure');
      $positionPreview.removeClass('failure');
      const sfen = $sfenInput.val();
      const variant = $variantSelect.val();
      $.ajax({
        url: $sfenInput.parent().data('validate-url'),
        data: {
          sfen: sfen,
          variant: variant,
        },
        success: function (data) {
          if (sfen) $sfenInput.addClass('success');
          $positionPreview.html(data);
          $position.find('.sfen_form a').attr('href', `editor/${variant}/${sfen}`);
          toggleButtons();
          li.pubsub.emit('content_loaded');
        },
        error: function () {
          $sfenInput.addClass('failure');
          $positionPreview.addClass('failure');
          $positionPreview.html('');
          $submits.prop('disabled', true);
        },
      });
    }, 300);
    $sfenInput.on('keyup', () => {
      setHandicapvalue();
      validateSfen();
      updateEngineName();
    });

    $positionInput.on('change', function (this: HTMLElement) {
      const position = $(this).val();
      if (position === 'fromPosition') {
        $positionWrap.show();
        $positionPreview.html(li.spinnerHtml);
        validateSfen();
      } else {
        $sfenInput.val('');
        $positionWrap.hide();
        $positionPreview.html();
        toggleButtons();
      }
      setHandicapvalue();
      updateEngineName();
      save();
    });

    $handicapSelect.on('change', function (): void {
      $sfenInput.val($handicapSelect.val());
      validateSfen();
      updateEngineName();
      save();
    });

    $variantSelect.on('change', function () {
      $positionPreview.html('');

      $default.click();
      $default.trigger('change');

      toggleButtons();
      updateHandicaps();
      save();
    });

    $modeChoices.on('change', save);

    $advancedTimeToggle.click(e => {
      e.preventDefault();
      if ($advancedTimeToggle.hasClass('active')) {
        $advancedTimeToggle.removeClass('active');
        $advancedTimeSetup.hide();
        resetIncSlider();
        resetPeriods();
        toggleButtons();
      } else {
        $advancedTimeToggle.addClass('active');
        $advancedTimeSetup.show();
      }
    });

    $form.find('div.level').each(function (this: HTMLElement) {
      $(this)
        .find('input')
        .on('change', () => {
          updateEngineName();
          save();
        });
    });

    const initForm = (): void => {
      const $variantOption = $variantSelect.find(`option[value="${this.root.opts.variant}"]`),
        sfen = !!$variantOption.length ? this.root.opts.sfen : undefined;

      if (sfen) $sfenInput.val(sfen.replace(/_/g, ' '));

      let hasSfen = !!$sfenInput.val();
      if (this.root.opts.variant) {
        if (hasSfen && !sfen) {
          $sfenInput.val('');
          hasSfen = false;
        }
        $variantOption.prop('selected', true);
      }
      if (hasSfen) {
        $fromPosition.click();
        $positionPreview.html(li.spinnerHtml);
        validateSfen();
      } else {
        $default.click();
        $positionWrap.hide();
      }
      updateEngineName();
      updateHandicaps();
      setHandicapvalue();
      initAdvancedTimeSetup();
      toggleButtons();
    };

    initForm();
  };
}
