// src/app/shared/pipes/safe-html.pipe.ts

import { Pipe, PipeTransform } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

@Pipe({
  name: 'safeHtml',
  standalone: true,
})
export class SafeHtmlPipe implements PipeTransform {
  constructor(private sanitizer: DomSanitizer) {}

  transform(value: string | null | undefined): SafeHtml {
    // Return an empty string if the value is null or undefined
    if (value == null) {
      return '';
    }
    // Sanitize and return the HTML
    return this.sanitizer.bypassSecurityTrustHtml(value);
  }
}