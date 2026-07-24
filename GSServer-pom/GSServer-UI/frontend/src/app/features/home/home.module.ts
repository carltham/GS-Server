import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
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
import { WebTerminalComponent } from './web-terminal.component';
import { UsersComponent } from './users.component';

@NgModule({
  declarations: [
    HomeComponent,
    HardeningComponent,
    ProxyComponent,
    ProxyInstallationComponent,
    WebTerminalComponent,
    UsersComponent
  ],
  imports: [
    SharedModule,
    FormsModule,
    HomeRoutingModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSlideToggleModule
  ]
})
export class HomeModule {}
