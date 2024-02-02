import { Preset } from './interfaces';

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

export function seekFromPreset(preset: Preset, anon: boolean) {
  const urlBase = preset.ai ? '/setup/ai/' : '/setup/hook/';
  return $.ajax({
    method: 'POST',
    url: urlBase + window.lishogi.sri,
    data: {
      variant: 1,
      timeMode: preset.timeMode,
      time: preset.lim,
      byoyomi: preset.byo,
      increment: preset.inc,
      periods: preset.per,
      days: preset.days,
      mode: anon ? 0 : 1,
      color: 'random',
    },
  });
}
