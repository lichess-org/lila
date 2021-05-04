'use strict';
Object.defineProperty(exports, '__esModule', { value: true });
exports.renderCombo = exports.playModifiers = void 0;
const snabbdom_1 = require('snabbdom');
const util_1 = require('../util');
const playModifiers = run => {
  const now = util_1.getNow();
  const malus = run.modifier.malus;
  const bonus = run.modifier.bonus;
  return {
    'puz-mod-puzzle': run.current.startAt > now - 90,
    'puz-mod-move': run.modifier.moveAt > now - 90,
    'puz-mod-malus-slow': !!malus && malus.at > now - 950,
    'puz-mod-bonus-slow': !!bonus && bonus.at > now - 950,
  };
};
exports.playModifiers = playModifiers;
const renderCombo = (config, renderBonus) => run => {
  const level = run.combo.level();
  return snabbdom_1.h('div.puz-combo', [
    snabbdom_1.h('div.puz-combo__counter', [
      snabbdom_1.h('span.puz-combo__counter__value', run.combo.current),
      snabbdom_1.h('span.puz-combo__counter__combo', 'COMBO'),
    ]),
    snabbdom_1.h('div.puz-combo__bars', [
      snabbdom_1.h('div.puz-combo__bar', [
        snabbdom_1.h('div.puz-combo__bar__in', {
          attrs: { style: `width:${run.combo.percent()}%` },
        }),
        snabbdom_1.h('div.puz-combo__bar__in-full'),
      ]),
      snabbdom_1.h(
        'div.puz-combo__levels',
        [0, 1, 2, 3].map(l =>
          snabbdom_1.h(
            'div.puz-combo__level',
            {
              class: {
                active: l < level,
              },
            },
            snabbdom_1.h('span', renderBonus(config.combo.levels[l + 1][1]))
          )
        )
      ),
    ]),
  ]);
};
exports.renderCombo = renderCombo;
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjoidXRpbC5qcyIsInNvdXJjZVJvb3QiOiIiLCJzb3VyY2VzIjpbIi4uL3NyYy92aWV3L3V0aWwudHMiXSwibmFtZXMiOltdLCJtYXBwaW5ncyI6Ijs7O0FBQUEsdUNBQTZCO0FBRzdCLGtDQUFpQztBQUUxQixNQUFNLGFBQWEsR0FBRyxDQUFDLEdBQVEsRUFBRSxFQUFFO0lBQ3hDLE1BQU0sR0FBRyxHQUFHLGFBQU0sRUFBRSxDQUFDO0lBQ3JCLE1BQU0sS0FBSyxHQUFHLEdBQUcsQ0FBQyxRQUFRLENBQUMsS0FBSyxDQUFDO0lBQ2pDLE1BQU0sS0FBSyxHQUFHLEdBQUcsQ0FBQyxRQUFRLENBQUMsS0FBSyxDQUFDO0lBQ2pDLE9BQU87UUFDTCxnQkFBZ0IsRUFBRSxHQUFHLENBQUMsT0FBTyxDQUFDLE9BQU8sR0FBRyxHQUFHLEdBQUcsRUFBRTtRQUNoRCxjQUFjLEVBQUUsR0FBRyxDQUFDLFFBQVEsQ0FBQyxNQUFNLEdBQUcsR0FBRyxHQUFHLEVBQUU7UUFDOUMsb0JBQW9CLEVBQUUsQ0FBQyxDQUFDLEtBQUssSUFBSSxLQUFLLENBQUMsRUFBRSxHQUFHLEdBQUcsR0FBRyxHQUFHO1FBQ3JELG9CQUFvQixFQUFFLENBQUMsQ0FBQyxLQUFLLElBQUksS0FBSyxDQUFDLEVBQUUsR0FBRyxHQUFHLEdBQUcsR0FBRztLQUN0RCxDQUFDO0FBQ0osQ0FBQyxDQUFDO0FBVlcsUUFBQSxhQUFhLGlCQVV4QjtBQUVLLE1BQU0sV0FBVyxHQUFHLENBQUMsTUFBYyxFQUFFLFdBQXNDLEVBQUUsRUFBRSxDQUFDLENBQUMsR0FBUSxFQUFTLEVBQUU7SUFDekcsTUFBTSxLQUFLLEdBQUcsR0FBRyxDQUFDLEtBQUssQ0FBQyxLQUFLLEVBQUUsQ0FBQztJQUNoQyxPQUFPLFlBQUMsQ0FBQyxlQUFlLEVBQUU7UUFDeEIsWUFBQyxDQUFDLHdCQUF3QixFQUFFO1lBQzFCLFlBQUMsQ0FBQyxnQ0FBZ0MsRUFBRSxHQUFHLENBQUMsS0FBSyxDQUFDLE9BQU8sQ0FBQztZQUN0RCxZQUFDLENBQUMsZ0NBQWdDLEVBQUUsT0FBTyxDQUFDO1NBQzdDLENBQUM7UUFDRixZQUFDLENBQUMscUJBQXFCLEVBQUU7WUFDdkIsWUFBQyxDQUFDLG9CQUFvQixFQUFFO2dCQUN0QixZQUFDLENBQUMsd0JBQXdCLEVBQUU7b0JBQzFCLEtBQUssRUFBRSxFQUFFLEtBQUssRUFBRSxTQUFTLEdBQUcsQ0FBQyxLQUFLLENBQUMsT0FBTyxFQUFFLEdBQUcsRUFBRTtpQkFDbEQsQ0FBQztnQkFDRixZQUFDLENBQUMsNkJBQTZCLENBQUM7YUFDakMsQ0FBQztZQUNGLFlBQUMsQ0FDQyx1QkFBdUIsRUFDdkIsQ0FBQyxDQUFDLEVBQUUsQ0FBQyxFQUFFLENBQUMsRUFBRSxDQUFDLENBQUMsQ0FBQyxHQUFHLENBQUMsQ0FBQyxDQUFDLEVBQUUsQ0FDbkIsWUFBQyxDQUNDLHNCQUFzQixFQUN0QjtnQkFDRSxLQUFLLEVBQUU7b0JBQ0wsTUFBTSxFQUFFLENBQUMsR0FBRyxLQUFLO2lCQUNsQjthQUNGLEVBQ0QsWUFBQyxDQUFDLE1BQU0sRUFBRSxXQUFXLENBQUMsTUFBTSxDQUFDLEtBQUssQ0FBQyxNQUFNLENBQUMsQ0FBQyxHQUFHLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FDdEQsQ0FDRixDQUNGO1NBQ0YsQ0FBQztLQUNILENBQUMsQ0FBQztBQUNMLENBQUMsQ0FBQztBQTlCVyxRQUFBLFdBQVcsZUE4QnRCIiwic291cmNlc0NvbnRlbnQiOlsiaW1wb3J0IHsgaCB9IGZyb20gJ3NuYWJiZG9tJztcbmltcG9ydCB7IFZOb2RlIH0gZnJvbSAnc25hYmJkb20vdm5vZGUnO1xuaW1wb3J0IHsgQ29uZmlnLCBSdW4gfSBmcm9tICcuLi9pbnRlcmZhY2VzJztcbmltcG9ydCB7IGdldE5vdyB9IGZyb20gJy4uL3V0aWwnO1xuXG5leHBvcnQgY29uc3QgcGxheU1vZGlmaWVycyA9IChydW46IFJ1bikgPT4ge1xuICBjb25zdCBub3cgPSBnZXROb3coKTtcbiAgY29uc3QgbWFsdXMgPSBydW4ubW9kaWZpZXIubWFsdXM7XG4gIGNvbnN0IGJvbnVzID0gcnVuLm1vZGlmaWVyLmJvbnVzO1xuICByZXR1cm4ge1xuICAgICdwdXotbW9kLXB1enpsZSc6IHJ1bi5jdXJyZW50LnN0YXJ0QXQgPiBub3cgLSA5MCxcbiAgICAncHV6LW1vZC1tb3ZlJzogcnVuLm1vZGlmaWVyLm1vdmVBdCA+IG5vdyAtIDkwLFxuICAgICdwdXotbW9kLW1hbHVzLXNsb3cnOiAhIW1hbHVzICYmIG1hbHVzLmF0ID4gbm93IC0gOTUwLFxuICAgICdwdXotbW9kLWJvbnVzLXNsb3cnOiAhIWJvbnVzICYmIGJvbnVzLmF0ID4gbm93IC0gOTUwLFxuICB9O1xufTtcblxuZXhwb3J0IGNvbnN0IHJlbmRlckNvbWJvID0gKGNvbmZpZzogQ29uZmlnLCByZW5kZXJCb251czogKGJvbnVzOiBudW1iZXIpID0+IHN0cmluZykgPT4gKHJ1bjogUnVuKTogVk5vZGUgPT4ge1xuICBjb25zdCBsZXZlbCA9IHJ1bi5jb21iby5sZXZlbCgpO1xuICByZXR1cm4gaCgnZGl2LnB1ei1jb21ibycsIFtcbiAgICBoKCdkaXYucHV6LWNvbWJvX19jb3VudGVyJywgW1xuICAgICAgaCgnc3Bhbi5wdXotY29tYm9fX2NvdW50ZXJfX3ZhbHVlJywgcnVuLmNvbWJvLmN1cnJlbnQpLFxuICAgICAgaCgnc3Bhbi5wdXotY29tYm9fX2NvdW50ZXJfX2NvbWJvJywgJ0NPTUJPJyksXG4gICAgXSksXG4gICAgaCgnZGl2LnB1ei1jb21ib19fYmFycycsIFtcbiAgICAgIGgoJ2Rpdi5wdXotY29tYm9fX2JhcicsIFtcbiAgICAgICAgaCgnZGl2LnB1ei1jb21ib19fYmFyX19pbicsIHtcbiAgICAgICAgICBhdHRyczogeyBzdHlsZTogYHdpZHRoOiR7cnVuLmNvbWJvLnBlcmNlbnQoKX0lYCB9LFxuICAgICAgICB9KSxcbiAgICAgICAgaCgnZGl2LnB1ei1jb21ib19fYmFyX19pbi1mdWxsJyksXG4gICAgICBdKSxcbiAgICAgIGgoXG4gICAgICAgICdkaXYucHV6LWNvbWJvX19sZXZlbHMnLFxuICAgICAgICBbMCwgMSwgMiwgM10ubWFwKGwgPT5cbiAgICAgICAgICBoKFxuICAgICAgICAgICAgJ2Rpdi5wdXotY29tYm9fX2xldmVsJyxcbiAgICAgICAgICAgIHtcbiAgICAgICAgICAgICAgY2xhc3M6IHtcbiAgICAgICAgICAgICAgICBhY3RpdmU6IGwgPCBsZXZlbCxcbiAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgIH0sXG4gICAgICAgICAgICBoKCdzcGFuJywgcmVuZGVyQm9udXMoY29uZmlnLmNvbWJvLmxldmVsc1tsICsgMV1bMV0pKVxuICAgICAgICAgIClcbiAgICAgICAgKVxuICAgICAgKSxcbiAgICBdKSxcbiAgXSk7XG59O1xuIl19
