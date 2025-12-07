import { Component, input, ChangeDetectionStrategy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-collapsible',
  imports: [CommonModule],
  templateUrl: './collapsible.component.html',
  styleUrl: './collapsible.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CollapsibleComponent {
  title = input<string>('Collapse Section');
  isCollapsed = signal(false);

  toggleCollapse() {
    this.isCollapsed.update(val => !val);
  }
}
