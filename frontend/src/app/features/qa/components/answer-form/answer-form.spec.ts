import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AnswerForm } from './answer-form';

describe('AnswerForm', () => {
  let component: AnswerForm;
  let fixture: ComponentFixture<AnswerForm>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AnswerForm]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AnswerForm);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
