import { Preset, PresetOpts } from './interfaces';

const headers = {
  Accept: 'application/vnd.lishogi.v3+json',
};

export function seeks() {
  return $.ajax({
    url: '/lobby/seeks',
    headers: headers,
  });
}

export function nowPlaying() {
  return $.ajax({
    url: '/account/now-playing',
    headers: headers,
  }).then(o => o.nowPlaying);
}

export function seekFromPreset(preset: Preset, opts: PresetOpts) {
  const data = {
    variant: '1',
    timeMode: preset.timeMode.toString(),
    time: preset.lim.toString(),
    byoyomi: preset.byo.toString(),
    increment: preset.inc.toString(),
    periods: preset.per.toString(),
    days: preset.days.toString(),
    mode: (opts.isAnon ? 0 : 1).toString(),
    ratingRange: opts.rating ? [opts.rating - opts.ratingDiff, opts.rating + opts.ratingDiff].join('-') : '',
    color: 'random',
  };
  if (preset.ai) {
    const d = new URLSearchParams({ ...data, sfen: '', level: preset.ai.toString(), position: 'default' }).toString();
    return $.ajax({
      method: 'POST',
      headers: {
        Accept: 'application/vnd.lishogi.v5+json',
      },
      url: '/setup/ai?redirect=1',
      processData: false,
      data: d,
      success: function (data) {
        window.lishogi.redirect(data);
      },
    });
  } else
    return $.ajax({
      method: 'POST',
      url: '/setup/hook/' + window.lishogi.sri,
      data: data,
    });
}
