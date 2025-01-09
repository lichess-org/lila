declare global {
  interface JQueryStatic {
    modal: LishogiModal;
    powerTip: any;
  }

  interface LishogiModal {
    (
      html: string | JQuery,
      cls?: string,
      onClose?: () => void,
      withDataAndEvents?: boolean
    ): JQuery;
    close(): void;
  }

  interface JQuery {
    powerTip(options?: PowerTip.Options | 'show' | 'hide'): JQuery;
    typeahead: any;
    friends: any;
    clock: any;
    watchers(): JQuery;
    watchers(method: 'set', data: any): void;
    spectrum(opts: any): any;
    infinitescroll(opts: any, f?: (...args: any[]) => any): any;
    toggleNone(v: boolean): void;
  }

  namespace PowerTip {
    type Placement =
      | 'n'
      | 'e'
      | 's'
      | 'w'
      | 'nw'
      | 'ne'
      | 'sw'
      | 'se'
      | 'nw-alt'
      | 'ne-alt'
      | 'sw-alt'
      | 'se-alt';

    interface Options {
      followMouse?: boolean;
      mouseOnToPopup?: boolean;
      placement?: Placement;
      smartPlacement?: boolean;
      popupId?: string;
      poupClass?: string;
      offset?: number;
      fadeInTime?: number;
      fadeOutTime?: number;
      closeDelay?: number;
      intentPollInterval?: number;
      intentSensitivity?: number;
      manual?: boolean;
      openEvents?: string[];
      closeEvents?: string[];
    }
  }
}

export {};
