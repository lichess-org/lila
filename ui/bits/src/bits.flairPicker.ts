import { Picker } from 'emoji-mart';

type Config = {
  element: HTMLElement;
  close: (e: PointerEvent) => void;
  onEmojiSelect: (i?: { id: string; src: string }) => void;
};

export async function initModule(cfg: Config): Promise<void> {
  if (cfg.element.classList.contains('emoji-done')) return;
  const theme =
    document.body.dataset.theme === 'system'
      ? 'auto'
      : document.body.dataset.theme === 'light'
        ? 'light'
        : 'dark';
  const opts = {
    ...cfg,
    onClickOutside: cfg.close,
    data: makeEmojiData,
    categories: categories.map(categ => categ[0]),
    categoryIcons,
    previewEmoji: 'people.backhand-index-pointing-up',
    noResultsEmoji: 'smileys.crying-face',
    skinTonePosition: 'none',
    theme,
    exceptEmojis: cfg.element.dataset.exceptEmojis?.split(' '),
  };
  const picker = new Picker(opts);

  cfg.element.prepend(picker as unknown as HTMLElement);
  cfg.element.classList.add('emoji-done');
  $(cfg.element).find('em-emoji-picker').attr('trap-bypass', '1'); // disable mousetrap within the shadow DOM
}

const makeEmojiData = async () => {
  const res = await fetch(site.asset.url('flair/list.txt', { pathVersion: true }));
  const text = await res.text();
  const lines = text.split('\n').slice(0, -1);
  const data = {
    categories: categories.map(([id, name]) => ({
      id: id,
      name: name,
      emojis: lines.filter(line => line.startsWith(id)),
    })),
    emojis: Object.fromEntries(
      lines.map(key => {
        const [categ, name] = key.split('.');
        return [
          key,
          {
            id: key,
            name: name,
            keywords: [categ, ...name.split('-')],
            skins: [
              {
                src: site.asset.flairSrc(key),
              },
            ],
          },
        ];
      }),
    ),
  };
  return data;
};

const categories: [string, string][] = [
  ['smileys', 'Smileys'],
  ['people', 'People'],
  ['nature', 'Animals & Nature'],
  ['food-drink', 'Food & Drink'],
  ['activity', 'Activity'],
  ['travel-places', 'Travel & Places'],
  ['objects', 'Objects'],
  ['symbols', 'Symbols'],
];

const categoryIcons = {
  smileys: {
    svg: $html`
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24">
        <path d="M12 0C5.373 0 0 5.373 0 12s5.373 12 12 12 12-5.373 12-12S18.627 0 12 0m0 22C6.486 22 2 17.514 2 12S6.486 2 12 2s10 4.486 10 10-4.486 10-10 10" />
        <path d="M8 7a2 2 0 1 0-.001 3.999A2 2 0 0 0 8 7M16 7a2 2 0 1 0-.001 3.999A2 2 0 0 0 16 7M15.232 15c-.693 1.195-1.87 2-3.349 2-1.477 0-2.655-.805-3.347-2H15m3-2H6a6 6 0 1 0 12 0" />
      </svg>`,
  },
  'food-drink': {
    svg: `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512"><path d="M481.9 270.1C490.9 279.1 496 291.3 496 304C496 316.7 490.9 328.9 481.9 337.9C472.9 346.9 460.7 352 448 352H64C51.27 352 39.06 346.9 30.06 337.9C21.06 328.9 16 316.7 16 304C16 291.3 21.06 279.1 30.06 270.1C39.06 261.1 51.27 256 64 256H448C460.7 256 472.9 261.1 481.9 270.1zM475.3 388.7C478.3 391.7 480 395.8 480 400V416C480 432.1 473.3 449.3 461.3 461.3C449.3 473.3 432.1 480 416 480H96C79.03 480 62.75 473.3 50.75 461.3C38.74 449.3 32 432.1 32 416V400C32 395.8 33.69 391.7 36.69 388.7C39.69 385.7 43.76 384 48 384H464C468.2 384 472.3 385.7 475.3 388.7zM50.39 220.8C45.93 218.6 42.03 215.5 38.97 211.6C35.91 207.7 33.79 203.2 32.75 198.4C31.71 193.5 31.8 188.5 32.99 183.7C54.98 97.02 146.5 32 256 32C365.5 32 457 97.02 479 183.7C480.2 188.5 480.3 193.5 479.2 198.4C478.2 203.2 476.1 207.7 473 211.6C469.1 215.5 466.1 218.6 461.6 220.8C457.2 222.9 452.3 224 447.3 224H64.67C59.73 224 54.84 222.9 50.39 220.8zM372.7 116.7C369.7 119.7 368 123.8 368 128C368 131.2 368.9 134.3 370.7 136.9C372.5 139.5 374.1 141.6 377.9 142.8C380.8 143.1 384 144.3 387.1 143.7C390.2 143.1 393.1 141.6 395.3 139.3C397.6 137.1 399.1 134.2 399.7 131.1C400.3 128 399.1 124.8 398.8 121.9C397.6 118.1 395.5 116.5 392.9 114.7C390.3 112.9 387.2 111.1 384 111.1C379.8 111.1 375.7 113.7 372.7 116.7V116.7zM244.7 84.69C241.7 87.69 240 91.76 240 96C240 99.16 240.9 102.3 242.7 104.9C244.5 107.5 246.1 109.6 249.9 110.8C252.8 111.1 256 112.3 259.1 111.7C262.2 111.1 265.1 109.6 267.3 107.3C269.6 105.1 271.1 102.2 271.7 99.12C272.3 96.02 271.1 92.8 270.8 89.88C269.6 86.95 267.5 84.45 264.9 82.7C262.3 80.94 259.2 79.1 256 79.1C251.8 79.1 247.7 81.69 244.7 84.69V84.69zM116.7 116.7C113.7 119.7 112 123.8 112 128C112 131.2 112.9 134.3 114.7 136.9C116.5 139.5 118.1 141.6 121.9 142.8C124.8 143.1 128 144.3 131.1 143.7C134.2 143.1 137.1 141.6 139.3 139.3C141.6 137.1 143.1 134.2 143.7 131.1C144.3 128 143.1 124.8 142.8 121.9C141.6 118.1 139.5 116.5 136.9 114.7C134.3 112.9 131.2 111.1 128 111.1C123.8 111.1 119.7 113.7 116.7 116.7L116.7 116.7z" /></svg>`,
  },
  'travel-places': {
    svg: `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512"><path d="M39.61 196.8L74.8 96.29C88.27 57.78 124.6 32 165.4 32H346.6C387.4 32 423.7 57.78 437.2 96.29L472.4 196.8C495.6 206.4 512 229.3 512 256V448C512 465.7 497.7 480 480 480H448C430.3 480 416 465.7 416 448V400H96V448C96 465.7 81.67 480 64 480H32C14.33 480 0 465.7 0 448V256C0 229.3 16.36 206.4 39.61 196.8V196.8zM109.1 192H402.9L376.8 117.4C372.3 104.6 360.2 96 346.6 96H165.4C151.8 96 139.7 104.6 135.2 117.4L109.1 192zM96 256C78.33 256 64 270.3 64 288C64 305.7 78.33 320 96 320C113.7 320 128 305.7 128 288C128 270.3 113.7 256 96 256zM416 320C433.7 320 448 305.7 448 288C448 270.3 433.7 256 416 256C398.3 256 384 270.3 384 288C384 305.7 398.3 320 416 320z"></path></svg>`,
  },
};
