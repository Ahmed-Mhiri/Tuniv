import { ComponentFixture, TestBed } from '@angular/core/testing';

import { QuestionListPageComponent } from './question-list-page.component';

describe('QuestionListPageComponent', () => {
  let component: QuestionListPageComponent;
  let fixture: ComponentFixture<QuestionListPageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [QuestionListPageComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(QuestionListPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
