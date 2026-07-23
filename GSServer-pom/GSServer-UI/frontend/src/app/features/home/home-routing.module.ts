import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { HomeComponent } from './home.component';
import { HardeningComponent } from './hardening.component';
import { ProxyComponent } from './proxy.component';
import { ProxyInstallationComponent } from './proxy-installation.component';

const routes: Routes = [
  {
    path: '',
    component: HomeComponent
  },
  {
    path: 'hardening',
    component: HardeningComponent
  },
  {
    path: 'proxy',
    component: ProxyComponent
  },
  {
    path: 'proxy/install',
    component: ProxyInstallationComponent
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class HomeRoutingModule {}
