const surveyLangs: Set<string> = new Set([
  'en',
  'ar',
  'zh-Hans',
  'cs',
  'nl',
  'fr',
  'de',
  'el',
  'it',
  'pt',
  'pt-BR',
  'ru',
  'es',
  'tr',
  'vi',
]);

const SURVEY_BASE = 'https://survey.lichess.org/index.php/';

function toLimeSurveyLang(code: string): string {
  const c = code.trim().replaceAll('_', '-');
  if (!c) return 'en';

  if (c === 'zh-Hans-CN' || c.startsWith('zh-Hans') || c === 'zh-CN' || c.startsWith('zh-CN'))
    return 'zh-Hans';
  if (c.startsWith('pt-PT')) return 'pt';
  if (c === 'pt' || c.startsWith('pt-BR')) return 'pt-BR';

  if (surveyLangs.has(c)) return c;

  const two = c.slice(0, 2);
  const mapped = two === 'pt' ? 'pt-BR' : two;
  return surveyLangs.has(mapped) ? mapped : 'en';
}

function pickBrowserLang(): string {
  for (const c of navigator.languages) {
    const mapped = toLimeSurveyLang(c);
    if (surveyLangs.has(mapped)) return mapped;
  }
  return 'en';
}

function surveyUrl(id: string, token: string, lang: string): string {
  const url = new URL(SURVEY_BASE + id);
  url.searchParams.set('lang', lang);
  url.searchParams.set('token', token);
  return url.href;
}

function fail(msg: string): void {
  const h1 = document.querySelector('main h1');
  if (h1) h1.textContent = msg;
}

site.load.then(() => {
  const params = new URLSearchParams(location.search);
  const id = params.get('id');
  const token = params.get('token');

  if (!id || !/^\d+$/.test(id)) fail('Invalid survey link.');
  else if (!token || !/^[A-Za-z0-9]{1,15}$/.test(token)) fail('Invalid survey link.');
  else {
    const langParam = params.get('lang');
    const lang = langParam ? toLimeSurveyLang(langParam) : pickBrowserLang();
    location.replace(surveyUrl(id, token, lang));
  }
});
