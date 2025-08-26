import { ChangeDetectionStrategy, Component } from '@angular/core';
import { ProfileEditPageComponent } from '../profile-edit-page/profile-edit-page.component';

// NG-ZORRO Imports
import { NzTabsModule } from 'ng-zorro-antd/tabs';
import { TwoFactorAuthSetupComponent } from '../../../auth/pages/two-factor-auth-setup/two-factor-auth-setup';
import { NzIconModule } from 'ng-zorro-antd/icon';

@Component({
  selector: 'app-settings-page',
  imports: [
    TwoFactorAuthSetupComponent, // The 2FA component we built
    ProfileEditPageComponent,    // The new edit form component
    NzTabsModule,
    NzIconModule
  ],
  templateUrl: './settings-page.component.html',
  styleUrl: './settings-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SettingsPageComponent {}