import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ServicesListComponent } from './services-list.component';

describe('ServicesList', () => {
  let component: ServicesListComponent;
  let fixture: ComponentFixture<ServicesListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ServicesListComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ServicesListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
