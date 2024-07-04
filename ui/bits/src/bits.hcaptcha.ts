export function initModule() {
  const script = document.createElement('script');
  script.src = 'https://hcaptcha.com/1/api.js';

  if ('credentialless' in window && window.crossOriginIsolated) {
    const documentCreateElement = document.createElement;
    script.src = 'https://hcaptcha.com/1/api.js?onload=initHcaptcha&recaptchacompat=off';
    script.onload = () => {
      document.createElement = function () {
        const element = documentCreateElement.apply(this, arguments as any);
        if (element instanceof HTMLIFrameElement) element.setAttribute('credentialless', '');
        return element;
      };
    };
    (window as any).initHcaptcha = () => (document.createElement = documentCreateElement);
  }

  document.head.appendChild(script);
}
