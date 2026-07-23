import { NgModule } from '@angular/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';

import { SharedModule } from '../../shared/shared.module';
import { HomeRoutingModule } from './home-routing.module';
import { HomeComponent } from './home.component';
import { HardeningComponent } from './hardening.component';
import { ProxyComponent } from './proxy.component';
import { ProxyInstallationComponent } from './proxy-installation.component';

@NgModule({
  declarations: [HomeComponent, HardeningComponent, ProxyComponent, ProxyInstallationComponent],
  imports: [
    SharedModule,
    HomeRoutingModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSlideToggleModule
  ]
})
export class HomeModule {}
