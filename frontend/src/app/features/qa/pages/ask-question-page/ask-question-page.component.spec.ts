import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AskQuestionPageComponent } from './ask-question-page.component';

describe('AskQuestionPageComponent', () => {
  let component: AskQuestionPageComponent;
  let fixture: ComponentFixture<AskQuestionPageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AskQuestionPageComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AskQuestionPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
