// Ensure all iframes created by 3rd-party scripts like Twitter or hcaptcha get the `credentialless` attribute.
export const patchCreateDocumentToEnsureAllIFramesAreCredentialless = () => {
  if (!window.crossOriginIsolated) return;

  const originalCreateElement = document.createElement;
  document.createElement = function () {
    const element = originalCreateElement.apply(this, arguments as any);
    if (element instanceof HTMLIFrameElement) {
      (element as any).credentialless = true;
    }
    return element;
  };
};
