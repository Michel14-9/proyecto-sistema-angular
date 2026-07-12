import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'keyValue',
  standalone: true
})
export class KeyValuePipe implements PipeTransform {
  transform(value: any): { key: string; value: any }[] {
    if (!value || typeof value !== 'object') {
      return [];
    }
    return Object.entries(value).map(([key, val]) => ({
      key: key,
      value: val
    }));
  }
}
