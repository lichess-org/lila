// Extend the global lichess object with a function defined in
// public/javascripts/insight-refresh.js
interface Lichess {
  refreshInsightForm(): void;
}

/* Type definitions for multiple-select.js */

// multiple-select.js uses jquery, but typescript thinks $ is Cash, not jquery.
// So as a hack, we extend the Cash interface knowing that at runtime, it will
// actually be a jquery object.
interface Cash {
  multipleSelect(method: 'getSelects'): string[];
  multipleSelect(method: 'setSelects', values: string[]): void;
  multipleSelect(
    method: 'enable' | 'disable' | 'open' | 'close' | 'checkAll' | 'uncheckAll' | 'focus' | 'blur' | 'refresh' | 'close'
  ): void;
  multipleSelect(option: MultiSelectOpts): void;
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
  multipleWidth?: number;
  single?: boolean;
  filter?: boolean;
  width?: string;
  dropWidth?: number;
  maxHeight?: string;
  container?: Element;
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
  styler?(): boolean;
  textTemplate?($elm: Element): string;
  labelTemplate?($elm: Element): string;
  onOpen?(): any;
  onClose?(): any;
  onCheckAll?(): any;
  onUncheckAll?(): any;
  onFocus?(): any;
  onBlur?(): any;
  onOptgroupClick?(): any;
  onClick?(view: { label: string; value: string; checked: boolean; instance: any }): any;
  onFilter?(): any;
}
