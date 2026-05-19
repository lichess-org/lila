/* Type definitions for multiple-select.js */

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
  onOpen?(): any;
  onClose?(): any;
  onCheckAll?(): any;
  onUncheckAll?(): any;
  onFocus?(): any;
  onBlur?(): any;
  onOptgroupClick?(view: { label: string; checked: boolean; children: EleLoose[]; instance: any }): any;
  onClick?(view: { label: string; value: string; checked: boolean; instance: any }): any;
  onFilter?(text: string): any;
}
