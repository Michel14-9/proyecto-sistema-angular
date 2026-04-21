import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MetricasCocina } from '../../models/cocinero.models';

@Component({
  selector: 'app-metricas-cocina',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './metricas-cocina.html',
  styleUrls: ['./metricas-cocina.css']
})
export class MetricasCocinaComponent {
  @Input() metricas!: MetricasCocina;
}