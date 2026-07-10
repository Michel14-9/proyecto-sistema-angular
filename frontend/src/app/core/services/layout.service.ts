// src/app/core/services/layout.service.ts
import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class LayoutService {
  private showHeaderSubject = new BehaviorSubject<boolean>(true);
  private showFooterSubject = new BehaviorSubject<boolean>(true);

  showHeader$ = this.showHeaderSubject.asObservable();
  showFooter$ = this.showFooterSubject.asObservable();

  hideHeaderAndFooter(): void {
    this.showHeaderSubject.next(false);
    this.showFooterSubject.next(false);
  }

  showHeaderAndFooter(): void {
    this.showHeaderSubject.next(true);
    this.showFooterSubject.next(true);
  }
}
