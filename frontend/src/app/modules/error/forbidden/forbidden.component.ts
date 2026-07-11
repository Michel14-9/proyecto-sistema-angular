import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { LayoutService } from '../../../core/services/layout.service';

@Component({
  selector: 'app-forbidden',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './forbidden.component.html',
  styleUrls: ['./forbidden.component.css']
})
export class ForbiddenComponent implements OnInit, OnDestroy {
  constructor(private layoutService: LayoutService) {}

  ngOnInit(): void {

    this.layoutService.hideHeaderAndFooter();
  }

  ngOnDestroy(): void {

    this.layoutService.showHeaderAndFooter();
  }

  goBack(): void {
    window.history.back();
  }
}
