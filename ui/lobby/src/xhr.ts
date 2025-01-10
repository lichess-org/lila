import { clockToPerf } from 'common/clock';
import debounce from 'debounce-promise';
import type { Preset, PresetOpts, Seek } from './interfaces';

export const seeks: () => Promise<Seek[]> = debounce(
  () => window.lishogi.xhr.json('GET', '/lobby/seeks'),
  2000,
);

export const nowPlaying: () => Promise<void> = () =>
  window.lishogi.xhr.json('GET', '/account/now-playing').then(o => o.nowPlaying);

export function seekFromPreset(preset: Preset, opts: PresetOpts): Promise<any> {
  const perf =
      preset.timeMode == 2
        ? 'correspondence'
        : clockToPerf(preset.lim * 60, preset.byo, preset.inc, preset.per),
    rating = opts.ratings?.[perf],
    data = {
      variant: '1',
      timeMode: preset.timeMode.toString(),
      time: preset.lim.toString(),
      byoyomi: preset.byo.toString(),
      increment: preset.inc.toString(),
      periods: preset.per.toString(),
      days: preset.days.toString(),
      mode: (opts.isAnon ? 0 : 1).toString(),
      ratingRange:
        rating && !rating.clueless
          ? [rating.rating - opts.ratingDiff, rating.rating + opts.ratingDiff].join('-')
          : '',
      color: 'random',
    };
  if (preset.ai) {
    return window.lishogi.xhr
      .json('POST', '/setup/ai', {
        url: {
          redirect: true,
        },
        formData: {
          ...data,
          sfen: '',
          level: preset.ai.toString(),
          position: 'default',
        },
      })
      .then(data => {
        window.lishogi.redirect(data);
      });
  } else
    return window.lishogi.xhr.json('POST', '/setup/hook/' + window.lishogi.sri, { formData: data });
}
