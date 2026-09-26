/* Type definitions for multipleSelect.js */

interface Cash {
  multipleSelect: MultipleSelect;
  multipleSelectDefaults: MultiSelectOpts;
  multipleSelectHover(fnOver: EventCallback, fnOut: EventCallback): Cash;
}

interface MultipleSelect {
  (method: 'getSelects'): string[];
  (method: 'setSelects', values: string[]): void;
  (
    method:
      | 'enable'
      | 'disable'
      | 'open'
      | 'close'
      | 'checkAll'
      | 'uncheckAll'
      | 'focus'
      | 'blur'
      | 'refresh',
  ): void;
  (option: MultiSelectOpts): void;
}

interface MultiSelectOpts {
  name?: string;
  isOpen?: boolean;
  placeholder?: string;
  selectAll?: boolean;
  selectAllDelimiter?: string[];
  minimumCountSelected?: number;
  ellipsis?: boolean;
  multiple?: boolean;
  /** Unit: pixels */
  multipleWidth?: number;
  single?: boolean;
  filter?: boolean;
  /** CSS string */
  width?: string;
  /** CSS string */
  dropWidth?: string;
  /** Unit: pixels */
  maxHeight?: number;
  position?: string;
  keepOpen?: boolean;
  animate?: string;
  displayValues?: boolean;
  delimiter?: string;
  addTitle?: boolean;
  filterAcceptOnEnter?: boolean;
  hideOptgroupCheckboxes?: boolean;
  selectAllText?: string;
  allSelected?: string;
  countSelected?: string;
  noMatchesFound?: string;
  styler?(value?: string): string | null;
  textTemplate?($elm: Cash): string;
  labelTemplate?($elm: Cash): string | null;
  onOpen?(): boolean | void;
  onClose?(): boolean | void;
  onCheckAll?(): boolean | void;
  onUncheckAll?(): boolean | void;
  onFocus?(): boolean | void;
  onBlur?(): boolean | void;
  onOptgroupClick?(view: {
    label: string;
    checked: boolean;
    children: EleLoose[];
    instance: any;
  }): boolean | void;
  onClick?(view: { label: string; value: string; checked: boolean; instance: any }): boolean | void;
  onFilter?(text: string): boolean;
}
