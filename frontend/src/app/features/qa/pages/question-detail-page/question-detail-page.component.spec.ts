import { ComponentFixture, TestBed } from '@angular/core/testing';

import { QuestionDetailPageComponent } from './question-detail-page.component';

describe('QuestionDetailPageComponent', () => {
  let component: QuestionDetailPageComponent;
  let fixture: ComponentFixture<QuestionDetailPageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [QuestionDetailPageComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(QuestionDetailPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
