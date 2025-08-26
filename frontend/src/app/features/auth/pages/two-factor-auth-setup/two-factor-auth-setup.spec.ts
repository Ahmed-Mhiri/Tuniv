import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TwoFactorAuthSetup } from './two-factor-auth-setup';

describe('TwoFactorAuthSetup', () => {
  let component: TwoFactorAuthSetup;
  let fixture: ComponentFixture<TwoFactorAuthSetup>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TwoFactorAuthSetup]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TwoFactorAuthSetup);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
