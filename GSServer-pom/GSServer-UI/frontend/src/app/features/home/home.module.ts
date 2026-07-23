import { NgModule } from '@angular/core';

import { SharedModule } from '../../shared/shared.module';
import { HomeRoutingModule } from './home-routing.module';
import { HomeComponent } from './home.component';
import { HardeningComponent } from './hardening.component';
import { ProxyComponent } from './proxy.component';

@NgModule({
  declarations: [HomeComponent, HardeningComponent, ProxyComponent],
  imports: [SharedModule, HomeRoutingModule]
})
export class HomeModule {}
