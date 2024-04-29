(window as any).initHcaptcha = () => {
  const div = document.querySelector('.h-captcha');
  if (!div) return;
  const parent = div.parentElement as HTMLElement;
  const next = div.nextSibling;
  div.remove();
  (window as any).hcaptcha.render(div);
  if ('credentialless' in window && window.crossOriginIsolated) {
    div.querySelector('iframe')?.setAttribute('credentialless', '');
  }
  parent.insertBefore(div, next);
};

export async function initModule() {
  const script = document.createElement('script');
  script.src = 'https://hcaptcha.com/1/api.js?onload=initHcaptcha&render=explicit';
  script.type = 'text/javascript';
  document.head.appendChild(script);
}
