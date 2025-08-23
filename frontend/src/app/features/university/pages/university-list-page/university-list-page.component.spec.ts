import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UniversityListPageComponent } from './university-list-page.component';

describe('UniversityListPageComponent', () => {
  let component: UniversityListPageComponent;
  let fixture: ComponentFixture<UniversityListPageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UniversityListPageComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(UniversityListPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
