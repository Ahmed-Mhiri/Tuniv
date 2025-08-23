import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ContentModerationPageComponent } from './content-moderation-page.component';

describe('ContentModerationPageComponent', () => {
  let component: ContentModerationPageComponent;
  let fixture: ComponentFixture<ContentModerationPageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ContentModerationPageComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ContentModerationPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
