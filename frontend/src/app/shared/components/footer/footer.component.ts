// src/app/layout/footer/footer.component.ts

import { NgOptimizedImage } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faGithub, faLinkedin, faTwitter } from '@fortawesome/free-brands-svg-icons';

@Component({
  selector: 'app-footer',
  standalone: true, // Explicitly define as standalone
  imports: [
    RouterLink,
    FaIconComponent // Import for icons
  ],
  templateUrl: './footer.component.html',
  styleUrl: './footer.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush // Add OnPush change detection
})
export class FooterComponent {
  // Use a single property for the year
  readonly currentYear = new Date().getFullYear();

  // Define links in the component class for easier maintenance
  readonly quickLinks = [
    { label: 'Universities', path: '/universities' },
    { label: 'Ask a Question', path: '/qa/ask' },
    { label: 'About Us', path: '/about' }
  ];

  readonly socialMediaLinks = [
    { name: 'GitHub', url: 'https://github.com/your-profile', icon: faGithub },
    { name: 'LinkedIn', url: 'https://linkedin.com/in/your-profile', icon: faLinkedin },
    { name: 'Twitter', url: 'https://twitter.com/your-profile', icon: faTwitter }
  ];
}