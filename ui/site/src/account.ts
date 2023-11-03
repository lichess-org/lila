import * as licon from 'common/licon';
import * as xhr from 'common/xhr';
import * as emojis from 'emoji-mart';

lichess.load.then(() => {
  $('.emoji-picker').each(function (this: HTMLElement) {
    const parent = this;
    const opts = {
      onEmojiSelect: console.log,
      data: async () => {
        return {
          categories: [
            {
              id: 'people',
              emojis: [
                'grinning',
                'smiley',
                'smile',
                'grin',
                'laughing',
                'sweat_smile',
                'rolling_on_the_floor_laughing',
                'joy',
                'slightly_smiling_face',
                'upside_down_face',
                'melting_face',
                'wink',
                'blush',
                'innocent',
                'smiling_face_with_3_hearts',
                'heart_eyes',
                'star-struck',
                'kissing_heart',
                'kissing',
                'relaxed',
                'kissing_closed_eyes',
                'kissing_smiling_eyes',
                'smiling_face_with_tear',
                'yum',
                'stuck_out_tongue',
                'stuck_out_tongue_winking_eye',
                'zany_face',
                'stuck_out_tongue_closed_eyes',
                'money_mouth_face',
                'hugging_face',
                'face_with_hand_over_mouth',
                'face_with_open_eyes_and_hand_over_mouth',
                'face_with_peeking_eye',
                'shushing_face',
                'thinking_face',
                'saluting_face',
                'zipper_mouth_face',
                'face_with_raised_eyebrow',
                'neutral_face',
                'expressionless',
                'no_mouth',
                'dotted_line_face',
                'face_in_clouds',
                'smirk',
                'unamused',
                'face_with_rolling_eyes',
                'grimacing',
                'face_exhaling',
                'lying_face',
                'relieved',
                'pensive',
                'sleepy',
                'drooling_face',
                'sleeping',
                'mask',
                'face_with_thermometer',
                'face_with_head_bandage',
                'nauseated_face',
                'face_vomiting',
                'sneezing_face',
                'hot_face',
                'cold_face',
                'woozy_face',
                'dizzy_face',
                'face_with_spiral_eyes',
                'exploding_head',
                'face_with_cowboy_hat',
                'partying_face',
                'disguised_face',
                'sunglasses',
                'nerd_face',
                'face_with_monocle',
                'confused',
                'face_with_diagonal_mouth',
                'worried',
                'slightly_frowning_face',
                'white_frowning_face',
                'open_mouth',
                'hushed',
                'astonished',
                'flushed',
                'pleading_face',
                'face_holding_back_tears',
                'frowning',
                'anguished',
                'fearful',
                'cold_sweat',
                'disappointed_relieved',
                'cry',
                'sob',
                'scream',
                'confounded',
                'persevere',
                'disappointed',
                'sweat',
                'weary',
                'tired_face',
                'yawning_face',
                'triumph',
                'rage',
                'angry',
                'face_with_symbols_on_mouth',
                'smiling_imp',
                'imp',
                'skull',
                'skull_and_crossbones',
                'hankey',
                'clown_face',
                'japanese_ogre',
                'japanese_goblin',
                'ghost',
                'alien',
                'space_invader',
                'robot_face',
                'wave',
                'raised_back_of_hand',
                'raised_hand_with_fingers_splayed',
                'hand',
                'spock-hand',
                'rightwards_hand',
                'leftwards_hand',
                'palm_down_hand',
                'palm_up_hand',
                'ok_hand',
                'pinched_fingers',
                'pinching_hand',
                'v',
                'crossed_fingers',
                'hand_with_index_finger_and_thumb_crossed',
                'i_love_you_hand_sign',
                'the_horns',
                'call_me_hand',
                'point_left',
                'point_right',
                'point_up_2',
                'middle_finger',
                'point_down',
                'point_up',
                'index_pointing_at_the_viewer',
                '+1',
                '-1',
                'fist',
                'facepunch',
                'left-facing_fist',
              ],
            },
          ],
        };
      },
    };
    const picker = new emojis.Picker(opts);
    parent.appendChild(picker as unknown as HTMLElement);
  });

  const localPrefs: [string, string, string, boolean][] = [
    ['behavior', 'arrowSnap', 'arrow.snap', true],
    ['behavior', 'courtesy', 'courtesy', false],
    ['behavior', 'scrollMoves', 'scrollMoves', true],
    ['notification', 'playBellSound', 'playBellSound', true],
  ];

  $('.security table form').on('submit', function (this: HTMLFormElement) {
    xhr.text(this.action, { method: 'post', body: new URLSearchParams(new FormData(this) as any) });
    $(this).parent().parent().remove();
    return false;
  });

  $('form.autosubmit').each(function (this: HTMLFormElement) {
    const form = this,
      $form = $(form),
      showSaved = () => $form.find('.saved').removeClass('none');
    computeBitChoices($form, 'behavior.submitMove');
    $form.find('input').on('change', function (this: HTMLInputElement) {
      computeBitChoices($form, 'behavior.submitMove');
      localPrefs.forEach(([categ, name, storeKey]) => {
        if (this.name == `${categ}.${name}`) {
          lichess.storage.boolean(storeKey).set(this.value == '1');
          showSaved();
        }
      });
      xhr.formToXhr(form).then(() => {
        showSaved();
        lichess.storage.fire('reload-round-tabs');
      });
    });
  });

  localPrefs.forEach(([categ, name, storeKey, def]) =>
    $(`#ir${categ}_${name}_${lichess.storage.boolean(storeKey).getOrDefault(def) ? 1 : 0}`).prop(
      'checked',
      true,
    ),
  );

  $('form[action="/account/oauth/token/create"]').each(function (this: HTMLFormElement) {
    const form = $(this),
      submit = form.find('button.submit');
    let isDanger = false;
    const checkDanger = () => {
      isDanger = !!form.find('.danger input:checked').length;
      submit.toggleClass('button-red', isDanger);
      submit.attr('data-icon', isDanger ? licon.CautionTriangle : licon.Checkmark);
      submit.attr('title', isDanger ? submit.data('danger-title') : '');
    };
    checkDanger();
    form.find('input').on('change', checkDanger);
    submit.on('click', function (this: HTMLElement) {
      return !isDanger || confirm(this.title);
    });
  });
});

function computeBitChoices($form: Cash, name: string) {
  let sum = 0;
  $form.find(`input[type="checkbox"][data-name="${name}"]:checked`).each(function (this: HTMLInputElement) {
    sum |= parseInt(this.value);
  });
  $form.find(`input[type="hidden"][name="${name}"]`).val(sum.toString());
}
