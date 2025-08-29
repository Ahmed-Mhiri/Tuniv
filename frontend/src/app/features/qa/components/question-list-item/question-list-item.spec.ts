import { ComponentFixture, TestBed } from '@angular/core/testing';

import { QuestionListItem } from './question-list-item';

describe('QuestionListItem', () => {
  let component: QuestionListItem;
  let fixture: ComponentFixture<QuestionListItem>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [QuestionListItem]
    })
    .compileComponents();

    fixture = TestBed.createComponent(QuestionListItem);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
