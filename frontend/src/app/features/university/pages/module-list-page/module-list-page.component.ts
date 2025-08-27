import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { switchMap } from 'rxjs';
import { UniversityService } from '../../services/university.service';
import { Module } from '../../../../shared/models/university.model';
import { SpinnerComponent } from '../../../../shared/components/spinner/spinner.component';

// NG-ZORRO Imports
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzTypographyModule } from 'ng-zorro-antd/typography';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzButtonModule } from 'ng-zorro-antd/button';

@Component({
  selector: 'app-module-list-page',
  imports: [
    RouterLink,
    SpinnerComponent,
    NzCardModule,
    NzAlertModule,
    NzTypographyModule,
    NzIconModule,
    NzButtonModule,
  ],
  templateUrl: './module-list-page.component.html',
  styleUrl: './module-list-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ModuleListPageComponent implements OnInit {
  // --- Dependencies ---
  private readonly universityService = inject(UniversityService);
  private readonly route = inject(ActivatedRoute);

  // --- State Signals ---
  readonly modules = signal<Module[]>([]);
  readonly universityName = signal<string>('');
  readonly isLoading = signal(true);
  readonly error = signal<string | null>(null);

  constructor() {
    // Get the university name passed via the router's state
    const navState = history.state;
    if (navState && navState.universityName) {
      this.universityName.set(navState.universityName);
    }
  }

  ngOnInit(): void {
    this.route.paramMap
      .pipe(
        switchMap((params) => {
          const universityId = Number(params.get('id'));
          this.isLoading.set(true);
          return this.universityService.getModulesByUniversity(universityId);
        })
      )
      .subscribe({
        next: (data) => {
          this.modules.set(data);
          this.isLoading.set(false);
        },
        error: () => {
          this.error.set('Failed to load modules. Please try again later.');
          this.isLoading.set(false);
        },
      });
  }
}