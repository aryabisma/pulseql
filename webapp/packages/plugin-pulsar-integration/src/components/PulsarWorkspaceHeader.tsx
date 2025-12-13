/*
 * CloudBeaver - Cloud Database Manager
 * Copyright (C) 2020-2025 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0.
 * you may not use this file except in compliance with the License.
 */

import { observer } from 'mobx-react-lite';
import { useService } from '@cloudbeaver/core-di';
import { PulsarUICustomizer } from '../PulsarUICustomizer.js';
import { WorkspaceModeService } from '../WorkspaceModeService.js';
import { BackToPulsarButton } from './BackToPulsarButton.js';

interface PulsarWorkspaceHeaderProps {
  children?: React.ReactNode;
}

/**
 * Header component for Pulsar workspace mode
 */
export const PulsarWorkspaceHeader = observer<PulsarWorkspaceHeaderProps>(
  function PulsarWorkspaceHeader({ children }) {
    const workspaceModeService = useService(WorkspaceModeService);
    const uiCustomizer = useService(PulsarUICustomizer);

    if (workspaceModeService.hideHeader) {
      return null;
    }

    const branding = workspaceModeService.getCustomBranding();

    return (
      <div className="pulsar-workspace-header">
        {branding?.logo && (
          <img 
            src={branding.logo} 
            alt={branding.title || 'Logo'} 
            className="workspace-logo"
          />
        )}
        {branding?.title && (
          <h1 className="workspace-title">{branding.title}</h1>
        )}
        <div className="header-spacer" />
        {children}
        <BackToPulsarButton />
      </div>
    );
  }
);
