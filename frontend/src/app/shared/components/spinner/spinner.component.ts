// src/app/components/spinner/spinner.component.ts

import { ChangeDetectionStrategy, Component, HostBinding, computed, input } from '@angular/core'; // 👈 1. Import 'computed'
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { trigger, transition, style, animate } from '@angular/animations';

@Component({
  selector: 'app-spinner',
  standalone: true,
  imports: [NzSpinModule],
  templateUrl: './spinner.component.html',
  styleUrl: './spinner.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  animations: [
    trigger('fade', [
      transition(':enter', [style({ opacity: 0 }), animate('200ms ease-in', style({ opacity: 1 }))]),
      transition(':leave', [animate('200ms ease-out', style({ opacity: 0 }))]),
    ]),
  ],
})
export class SpinnerComponent {
  isLoading = input<boolean>(false);
  isOverlay = input<boolean>(true, { alias: 'overlay' });
  tip = input<string>(); // This is type 'string | undefined'
  size = input<'small' | 'default' | 'large'>('large');
  
  // 👇 2. Create a computed signal to safely transform the type
  // It reads the 'tip' signal and converts any 'undefined' value to 'null'.
  readonly nzTip = computed(() => this.tip() ?? null);

  @HostBinding('class.is-inline') get isInline() {
    return !this.isOverlay();
  }
}