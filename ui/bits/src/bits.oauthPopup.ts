export function initModule(args: {
  platform: string;
  href: string;
  result: string | Record<string, string>;
}): void {
  if (window.opener && !window.opener.closed) {
    window.opener.postMessage({ ...args, ok: true }, window.location.origin);
    window.close();
  } else {
    window.location.href = args.href;
  }
}
