export async function initModule(args: { selector: string; ops: string }) {
  await site.load;
  const el = document.querySelector(args.selector) as HTMLElement;
  const oplist = args.ops.split(' ');
  if (!el || oplist.length === 0) return;
  if (oplist.includes('focus')) el.focus(); // yes i know about autofocus attribute
  if (oplist.includes('begin')) (el as HTMLInputElement).setSelectionRange(0, 0);

  // this behavior was needed but it was tough to justify as a separate module, so let's pretend it
  // will one day serve other form-related functions dictated by the server that require js
}
